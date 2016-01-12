#include "usb_tethering.h"

#define TAG "usb_tethering"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)
#define TIMEOUT 60 //seconds

typedef enum
{
	OFF=0,
	INIT=1,
	READY=2,
	CONNECT=3,
	UNKNOWN=4
} state_t;

state_t state = UNKNOWN;

/**
 * Used to store all three pthreads to kill them later if needed.
 */
pthread_t tid[3];
/**
 * Hold the ip of the desktop pc if connected.
 */
char* client_ip = NULL;

pthread_cond_t  state_cond  = PTHREAD_COND_INITIALIZER;
pthread_mutex_t state_mutex = PTHREAD_MUTEX_INITIALIZER;


/**
 * Execute a system command and return it's exit value.
 */
int execute(char* command)
{
	return system(command);
}

/**
 * Splits a string with the given delimiter and return array of char*.
 */
char** str_split(char* a_str, const char a_delim)
{
    char** result    = 0;
    size_t count     = 0;
    char* tmp        = a_str;
    char* last_comma = 0;
    char delim[2];
    delim[0] = a_delim;
    delim[1] = 0;

    // count how many elements will be extracted.
    while (*tmp)
    {
        if (a_delim == *tmp)
        {
            count++;
            last_comma = tmp;
        }
        tmp++;
    }

    // add space for trailing token.
    count += last_comma < (a_str + strlen(a_str) - 1);
    count++;

    result = malloc(sizeof(char*) * count);

    if (result)
    {
        size_t idx  = 0;
        char* token = strtok(a_str, delim);

        while (token)
        {
            assert(idx < count);
            *(result + idx++) = strdup(token);
            token = strtok(0, delim);
        }
        assert(idx == count - 1);
        *(result + idx) = 0;
    }

    return result;
}

void change_state(state_t _state) {

	pthread_mutex_lock(&state_mutex);
	state = _state;
	pthread_cond_signal(&state_cond);
	pthread_mutex_unlock(&state_mutex);
}

/**
 * Kill an open pipe which was opened with popen.
 */
void* kill_pipe(void *fp)
{
	if(fp!=NULL) {
		LOGI("return value of pipe: %i", pclose(fp));
	}else{
		LOGI("pipe is null");
	}

	//pthread_mutex_unlock(&logcat_lock);
	//pthread_exit(NULL);
}

/**
 * Wait till the timeout (which is defined in header) is reached and if no connection is established, call stop_tethering().
 */
void timeout()
{
	int n = 0;

	LOGD("timeout");
	//timeout till client_ip was found or timeout reached
	while(client_ip==NULL && n++ <= TIMEOUT) {
		sleep(1);
	}

	LOGD("timeout done, client_ip=%s, count=%d", client_ip, n);
}

/**
 * Clear and set the ip address of a client which dnsmasq has written to the logcat.
 */
void* logcat()
{
	FILE *fp;
	char line[1024];

	//pthread_mutex_lock(&logcat_lock);
	//reset logcat
	LOGD("clear logcat");
	if(execute("su -c 'logcat -c'")) {
		LOGE("clear logcat failed");
	}else{
		LOGD("check output of dnsmasq");
		fp = popen("su -c 'logcat -s dnsmasq'", "r");
		if (fp == NULL){
			LOGE("logcat failed");

		}else{
			/* Read the output a line at a time - output it. */
			while (fgets(line, sizeof(line)-1, fp) != NULL) {
				LOGI("line: %s", line);
				if (strstr(line, "DHCPACK(rndis0)") != NULL) {
					// contains
					LOGD("contains DHCPACK(rndis0)");
					//I/dnsmasq ( 1838): DHCPOFFER(rndis0) 192.168.42.13 16:6e:a4:54:ae:31
					//TODO not necessary and not really stable in my opinion
					char** tokens;
					tokens = str_split(line, ' ');
					int n = sizeof(tokens)/sizeof(tokens[0]);
					client_ip = tokens[4];

					break;
				}

				LOGI("wait for next line");
			}

			//close pipe in pthread, cause it could take a lot of time...
			if(fp!=NULL) {
				LOGD("close pipe in new thread");
				if (pthread_create(&(tid[1]), NULL, &kill_pipe, (void *)&fp))
				{
					LOGE("pthread_create failed");
				}
			}
		}
	}

	LOGD("exit logcat");
	//pthread_exit(NULL);
}

/**
 * Use 'netcfg' to check if usb-tethering is active.
 */
bool netcfg()
{
	FILE *fp;
	char line[1024];
	bool active = false;

	//pthread_mutex_lock(&logcat_lock);
	LOGD("check output of netcfg");
	//use 'grep' to be faster ;)
	fp = popen("su -c 'netcfg | grep rndis0'", "r");
	if (fp == NULL){
		LOGE("netcfg failed");
	}else{

		/* Read the output a line at a time - output it. */
		while (fgets(line, sizeof(line)-1, fp) != NULL) {
			LOGI("line: %s", line);
			if (strstr(line, "rndis0") != NULL && strstr(line, "UP") != NULL) {
				// active!
				LOGD("rndis0 is UP");
				change_state(READY);

				active = true;
				//TODO - check if connected!
				break;
			}
		}
	}

	//close pipe in pthread, cause it could take a lot of time...
	LOGD("close pipe in new thread");
	pthread_t id;
	if (pthread_create(&id, NULL, &kill_pipe, (void *)&fp))
	{
		LOGE("pthread_create failed");
	}

	return active;
}

/**
 * Run the dnsmasq command which acts like a DNS server and give the client a ip address out of the pool.
 */
void* dnsmasq()
{
	LOGD("dnsmasq");
	//--address=/#//192.168.42.1 - will redirect every http to server
	if(execute("su -c 'dnsmasq --no-daemon --no-poll --no-resolv --interface=rndis0 --address=/one.damn/192.168.42.1 --address=/www.one.damn/192.168.42.1 --dhcp-range=192.168.42.10,192.168.42.100,2h'")) {
		LOGE("dnsmasq failed");
	}

	LOGI("dnsmasq ended");
	//pthread_exit(NULL);
}


char* start_tethering()
{
	//check if usb-tethering is active TODO ???
	if(is_tethering_active()) {
		LOGD("usb-tethering is active");

		return "unknown host";
	}

	LOGD("start_tethering");
	change_state(INIT);

	//'setprop' - does not have to be checked explicit because we check with ifconfig
	int ret = 0;
	ret = execute("su -c 'touch /data/local/tmp/test1'");
	if(ret)
	{
		LOGE("touch failed, val=%d", ret);
		stop_tethering();
		return NULL;
	}

	execute("su -c 'setprop sys.usb.config rndis,adb'");
	ret=execute("su -c 'ifconfig rndis0 192.168.42.1 netmask 255.255.255.0 up'");
	if(ret)
	{
		LOGE("ifconfig failed, val=%d",ret);
		stop_tethering();
		return NULL;
	}

	if(execute("su -c 'route add default gw 192.168.42.2 dev rndis0'"))
	{
		LOGE("route failed");
		stop_tethering();
		return NULL;
	}

	change_state(READY);

	LOGI("start logcat");
	//first thread for checking logcat
	if (pthread_create(&(tid[0]), NULL, &logcat, NULL))
	{
		LOGE("pthread_create failed");
		stop_tethering();
		return NULL;
	}

	LOGI("start dnsmasq");
	//second thread for dnsmasq
	if (pthread_create(&(tid[1]), NULL, &dnsmasq, NULL))
	{
		LOGE("pthread_create failed");
		stop_tethering();
		return NULL;
	}

	//wait for timeout or client is connected TODO should we have a timeout for this? may longer...
	timeout();

	//something went wrong
	if(client_ip==NULL)
		stop_tethering();

	change_state(CONNECT);
	return client_ip;
}

int stop_tethering()
{
	LOGD("stop_tethering");
	execute("su -c 'ifconfig rndis0 192.168.42.1 netmask 255.255.255.0 down'");
	client_ip = NULL;
	change_state(OFF);

	return execute("su -c 'setprop sys.usb.config adb'");
}

bool is_tethering_active()
{
	return netcfg();
}

jstring Java_at_fhooe_mcm14_damn_jni_USBTethering_startUSBTetheringJNI(JNIEnv* env)
{
	return (*env)->NewStringUTF(env, start_tethering());
}

bool Java_at_fhooe_mcm14_damn_jni_USBTethering_stopUSBTetheringJNI()
{
	return stop_tethering()==0;
}

bool Java_at_fhooe_mcm14_damn_jni_USBTethering_checkUSBTetheringJNI()
{
	return is_tethering_active();
}

int Java_at_fhooe_mcm14_damn_jni_USBTethering_getStateBlockingJNI()
{
	//block until something changed
	pthread_mutex_lock(&state_mutex);
	pthread_cond_wait(&state_cond, &state_mutex);
	pthread_mutex_unlock(&state_mutex);

	return state;
}

int Java_at_fhooe_mcm14_damn_jni_USBTethering_getStateJNI()
{
	return state;
}

//not needed if we do not initialize anything
jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env;
	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
		return -1;

    LOGI("jni version: %d", JNI_VERSION_1_6);

    return JNI_VERSION_1_6;
}
