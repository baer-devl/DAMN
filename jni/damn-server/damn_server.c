#include "damn_server.h"

#define TAG "damn-server"
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, TAG,__VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)
#define UNUSED(x) (void)(x)
#define null NULL
#define DAMN_SERVER_FILE "DAMNSERVERLOCK"
#define NOTIFICATION_FILE "DAMNSERVERNOTIFICATIONFILE"
#define MSG_FILE "DAMNSERVERMESSENGEFILE"
#define RECEIVE_FILE "DAMNSERVERRECEIVEFILE"
#define MAX_BUFF_FIFO 4096

typedef enum
{
	OFF=0,
	INIT=1,
	RUNNING=2,
	UNKNOWN=3
} state_t;

static state_t state = UNKNOWN;
const char *mCacheDirectory = "/data/local/tmp";

struct mg_context *ctx; //TODO static needed?

// simple structure for keeping track of websocket connection
struct ws_connection {
    struct mg_connection    *conn;
    char 					*app;
    char					*sApp;
    char					*sThread;
    int     				 closing;
};

//static struct session sessions[MAX_SESSIONS];
static pthread_rwlock_t rwlock = PTHREAD_RWLOCK_INITIALIZER;

#define CONNECTIONS 20
static struct ws_connection ws_conn[CONNECTIONS];

static void change_state(state_t _state)
{
	LOGI("new state = %d", _state);
	char file[256];
	char buf[MAX_BUFF_FIFO];
	sprintf(file, "%s/%s", mCacheDirectory, NOTIFICATION_FILE);
	sprintf(buf, "%d", _state);
	FILE *fp;

	pthread_rwlock_wrlock(&rwlock);

	/* create the FIFO (named pipe) */
	if(access(file, F_OK) == -1){
		LOGI("no state file1");
		LOGI("create file: %s", file);
		umask(0);
		mknod(file, S_IFIFO|0666, 0);
	}

	if((fp = fopen(file, "r+")) == NULL) {
		LOGE("fopen failed2");
		pthread_rwlock_unlock(&rwlock);

		return;
	}
	fputs(buf, fp);
	fclose(fp);
	LOGI("new state = %d", _state);
	state = _state;

	pthread_rwlock_unlock(&rwlock);
}

//begin request
//here example for POST
static int begin_request_handler(struct mg_connection *conn)
{
    const struct mg_request_info *ri = mg_get_request_info(conn);
    LOGI("new request url: '%s'", ri->uri);

    if (!strcmp(ri->uri, "/client_pin")) {
    	char post_data[20], pin[10];
		int post_data_len;
    	LOGI("got post");
        /*// User has submitted a form, show submitted data and a variable value
        post_data_len = mg_read(conn, post_data, sizeof(post_data));

        // Parse form data. input1 and input2 are guaranteed to be NUL-terminated
        mg_get_var(post_data, post_data_len, "input_1", input1, sizeof(input1));
        mg_get_var(post_data, post_data_len, "input_2", input2, sizeof(input2));

        // Send reply to the client, showing submitted form values.*/


    	//handle the pin data
    	post_data_len = mg_read(conn, post_data, sizeof(post_data));
    	LOGI("post data:'%s'", post_data);

    	// Parse form data. input1 and input2 are guaranteed to be NUL-terminated
		mg_get_var(post_data, post_data_len, "pin", pin, sizeof(pin));
		LOGI("pin='%s'", pin);

		//show notify

		//redirect if accept, redirect to looser lounge otherwise //TODO
		char *html;
    	html = "<html><body><h1>hello</h1></body></html>";

        // Show HTML form.
        mg_printf(conn, "HTTP/1.0 200 OK\r\n"
                  "Content-Length: %d\r\n"
                  "Content-Type: text/html\r\n\r\n%s",
                  (int) strlen(html), html);


    } else if(!strcmp(ri->uri, "/index.html")) {
    	//let civetweb handle this
        return 0;

    } else if(!strcmp(ri->uri, "/rules")) {
		char *html;
		html = "<html><body>awesomeness!</body></html>";

		// Show HTML form.
		mg_printf(conn, "HTTP/1.0 200 OK\r\n"
				  "Content-Length: %d\r\n"
				  "Content-Type: text/html\r\n\r\n%s",
				  (int) strlen(html), html);

		//done with it
		return 1;

	} else if(!strcmp(ri->uri, "/ws")) {
    	LOGI("request: %s %s", ri->uri, ri->request_method);
    	return 0; //will not proceeded -> websocket

    } else if(!strcmp(ri->uri, "/whooteva")){
    	LOGI("request...: %s", ri->uri);
    	size_t post_len = ri->content_length;
    	char post_data[post_len], pin[20];
		int post_data_len;

    	//handle the pin data
		post_data_len = mg_read(conn, post_data, post_len);
		LOGI("request len: %d:%d", post_len, post_data_len);

		if(post_len!=0) {
			int ss = mg_get_var(post_data, post_len, "pin", pin, sizeof(pin));
			char ppin[ss+1];
			strncpy(ppin, pin, ss);
			ppin[ss] = '\0'; //cause mg doesnt care about termination...
			LOGI("pin:'%s':%d", ppin, ss);
		}

		char *html =
				"<html><body>awesomeness!</body></html>";

		// response
		mg_printf(conn, "HTTP/1.0 200 OK\r\n"
						"Content-Length: %d\r\n"
						"Content-Type: text/html\r\n\r\n%s",
						 (int) strlen(html), html);

		return 1;

    } else if(!strcmp(ri->uri, "/test")) {
    	char *html_form =
    	    "<html><body>POST example."
    	    "<form method=\"POST\" action=\"/blubb\">"
    	    "Input 1: <input type=\"text\" name=\"input_1\" /> <br/>"
    	    "Input 2: <input type=\"text\" name=\"input_2\" /> <br/>"
    	    "<input type=\"submit\" />"
    	    "</form></body></html>";

    	mg_printf(conn, "HTTP/1.0 200 OK\r\n"
    	                "Content-Length: %d\r\n"
    	                "Content-Type: text/html\r\n\r\n%s",
    	                 (int) strlen(html_form), html_form);

    	return 1;


    } else if(!strcmp(ri->uri, "/blubb")) {

    	char post_data[1024], input1[sizeof(post_data)], input2[sizeof(post_data)];
    	int post_data_len;

    	post_data_len = ri->content_length;

        mg_read(conn, post_data, post_data_len);

        // Parse form data. input1 and input2 are guaranteed to be NUL-terminated
        mg_get_var(post_data, post_data_len, "input_1", input1, sizeof(input1));
        mg_get_var(post_data, post_data_len, "input_2", input2, sizeof(input2));

    	mg_printf(conn, "HTTP/1.0 200 OK\r\n"
    	                  "Content-Type: text/plain\r\n\r\n"
    	                  "Submitted data: [%.*s]\n"
    	                  "Submitted data length: %d bytes\n"
    	                  "input_1: [%s]\n"
    	                  "input_2: [%s]\n",
    	                  post_data_len, post_data, post_data_len, input1, input2);
    	return 1;


    } else {
    	LOGI("request: %s", ri->uri);

		return 0;
    }

    return 1;  // Mark request as processed
}

static void *read_thread(void *parm)
{
	int wsd = (long)parm;
	struct mg_connection *conn = ws_conn[wsd].conn;

	FILE *fd;
	char file[256];
	char buf[MAX_BUFF_FIFO];
	if(ws_conn[wsd].sThread == NULL)
		sprintf(file, "/data/data/%s/%s_%s", ws_conn[wsd].sApp, MSG_FILE, ws_conn[wsd].sApp);
	else
		sprintf(file, "/data/data/%s/%s_%s?%s", ws_conn[wsd].sApp, MSG_FILE, ws_conn[wsd].sApp, ws_conn[wsd].sThread);
	LOGI("read thread: %s", file);

	//read from fifo
	if(access(file, F_OK) == -1){
		LOGI("no notification file");
		umask(0);
		mknod(file, S_IFIFO|0666, 0);
	}

	while(true)
	{
		fd = fopen(file, "r");
		if(fd == null) {
			LOGE("fopen error");
			return NULL;
		}

		if(fgets(buf, MAX_BUFF_FIFO, fd) == NULL)
			break;

		fclose(fd);

		if(conn){
//			LOGI("push to wss: %s  |%s|", file, buf);
			mg_websocket_write(conn, WEBSOCKET_OPCODE_TEXT, buf, strlen(buf));
		}
		else
			break;
	}

	LOGI("read thread closed");

	return NULL;
}

static void *ws_server_thread(void *parm)
{
    int wsd = (long)parm;
    struct mg_connection *conn = ws_conn[wsd].conn;

    //create listener for pushing msg
    pthread_t da_read_thread;
    if(pthread_create(&da_read_thread, NULL, read_thread, (void *)(long)wsd)) {
		LOGI("Error creating thread");
    }

    /* While the connection is open, send periodic updates */
    while(!ws_conn[wsd].closing) {
        sleep(1); /* 1 second */

        /* Send periodic PING to assure websocket remains connected, except if we are closing */
        if (!ws_conn[wsd].closing)
            mg_websocket_write(conn, WEBSOCKET_OPCODE_PING, NULL, 0);
    }

    fprintf(stderr, " %d exiting\n", wsd);

    //remove files
    char file[256];

	//delete file if exist
	sprintf(file, "/data/data/%s/%s_%s?%s", ws_conn[wsd].sApp, MSG_FILE, ws_conn[wsd].sApp, ws_conn[wsd].sThread);
	remove(file);
	sprintf(file, "/data/data/%s/%s_%s?%s", ws_conn[wsd].sApp, RECEIVE_FILE, ws_conn[wsd].sApp, ws_conn[wsd].sThread);
    remove(file);

    // reset connection information to allow reuse by new client
    ws_conn[wsd].conn = NULL;
    ws_conn[wsd].app = NULL;
    ws_conn[wsd].closing = 2;

    return NULL;
}

// websocket_connect_handler()
static int websocket_connect_handler(const struct mg_connection *conn)
{
    int i;
    struct mg_request_info *ri = mg_get_request_info(conn);
	LOGI("ws connect handler '%s'", ri->uri);
	LOGI("query: %s", ri->query_string);

    fprintf(stderr, "connect handler\n");

    for(i=0; i < CONNECTIONS; ++i) {
        if (ws_conn[i].conn == NULL) {
//            LOGI("...prep for server %d", i);
            ws_conn[i].conn = (const struct mg_connection *)conn;
            ws_conn[i].closing = 0;
            ws_conn[i].app = ri->query_string;

            char *p;
            p = strtok(ri->query_string, "?");
			ws_conn[i].sApp = p;
            p = strtok(NULL, "?");
            ws_conn[i].sThread = p;

            LOGI("query: %s", ws_conn[i].app);
            LOGI("app: %s", ws_conn[i].sApp);
            LOGI("thread: %s", ws_conn[i].sThread);

            break;
        }
    }
    if (i >= CONNECTIONS) {
    	LOGE("Refused connection: Max connections exceeded");
        return 1;
    }

    return 0;
}

// websocket_ready_handler()
static void websocket_ready_handler(struct mg_connection *conn)
{
    int i;

    fprintf(stderr, "ready handler\n");
    for(i=0; i < CONNECTIONS; ++i) {
        if (ws_conn[i].conn == conn) {
            fprintf(stderr, "...start server %d\n", i);
            char file[256];
			sprintf(file, "%s/%s_%s?%s", mCacheDirectory, RECEIVE_FILE, ws_conn[i].sApp, ws_conn[i].sThread);
			pthread_rwlock_wrlock(&rwlock);

			/* create the FIFO (named pipe) */
			if(access(file, F_OK) == -1){
				LOGI("no receive file");
				umask(0);
				mkfifo(file, 0666);
			}

			pthread_rwlock_unlock(&rwlock);


            mg_start_thread(ws_server_thread, (void *)(long)i);
            break;
        }
    }
}

// websocket_close_handler()
static void websocket_close_handler(struct mg_connection *conn)
{
    int i;

    for(i=0; i < CONNECTIONS; ++i) {
        if (ws_conn[i].conn == conn) {
            fprintf(stderr, "...close server %d\n", i);
            LOGI("close connection");
            ws_conn[i].closing = 1;
        }
    }
}

static int websocket_data_handler(struct mg_connection *conn, int flags,
                                  char *data, size_t data_len)
{
    int i;
    int wsd;

    for(i=0; i < CONNECTIONS; ++i) {
        if (ws_conn[i].conn == conn) {
            wsd = i;
            break;
        }
    }
    if (i >= CONNECTIONS) {
        fprintf(stderr, "Received websocket data from unknown connection\n");
        return 1;
    }

    if (flags & 0x80) {
        flags &= 0x7f;

        switch (flags) {
        case WEBSOCKET_OPCODE_CONTINUATION:
            fprintf(stderr, "CONTINUATION...\n");
            break;
        case WEBSOCKET_OPCODE_TEXT:

        	pthread_rwlock_wrlock(&rwlock);
        	char file[256];

			sprintf(file, "/data/data/%s/%s_%s?%s", ws_conn[wsd].sApp, RECEIVE_FILE, ws_conn[wsd].sApp, ws_conn[wsd].sThread);
			FILE *fp;

			if((fp = fopen(file, "r+")) == NULL) {
				LOGE("fopen failed233: %s", file);
				pthread_rwlock_unlock(&rwlock);

				break;
			}

			fputs(data, fp);
			fflush(fp);
			fclose(fp);

//			LOGD("got data from wss: |%s|:%d from %d put it to: %s", data, data_len, getuid(), file);

			pthread_rwlock_unlock(&rwlock);
            break;
        case WEBSOCKET_OPCODE_BINARY:
            fprintf(stderr, "BINARY...\n");
            break;
        case WEBSOCKET_OPCODE_CONNECTION_CLOSE:
            fprintf(stderr, "CLOSE...\n");
            if (!ws_conn[wsd].closing) {
                mg_websocket_write(conn, WEBSOCKET_OPCODE_CONNECTION_CLOSE, data, data_len);
                ws_conn[wsd].closing = 1;
            }

            /* time to close the connection */
            return 0;
            break;
        case WEBSOCKET_OPCODE_PING:
            /* client sent PING, respond with PONG */
            mg_websocket_write(conn, WEBSOCKET_OPCODE_PONG, data, data_len);
            break;
        case WEBSOCKET_OPCODE_PONG:
            /* received PONG to our PING, no action */
            break;
        default:
            fprintf(stderr, "Unknown flags: %02x\n", flags);
            break;
        }
    }

    /* keep connection open */
    return 1;
}


void start_server(const char *docroot, const char *pemfile, const char *cachedir)
{
	LOGD("start_server");
	change_state(INIT);
    char server_name[40];
    mCacheDirectory = cachedir;

    struct mg_callbacks callbacks;
    const char *options[] = {
		"document_root", docroot,
        "listening_ports", "80r,443s", //redirect port 80 to secured 443
        "ssl_certificate", pemfile,
        "num_threads", "5",
        NULL
    };

    LOGI("docroot: %s, pemfile: %s, tmp: %s", docroot, pemfile, cachedir);

    /* get simple greeting for the web server */
    snprintf(server_name, sizeof(server_name),
             "Civetweb websocket server v. %s",
             mg_version());

    memset(&callbacks, 0, sizeof(callbacks));

    //http handler
    callbacks.begin_request = begin_request_handler;

    //websocket handler
    callbacks.websocket_connect = websocket_connect_handler;
    callbacks.websocket_ready = websocket_ready_handler;
    callbacks.websocket_data = websocket_data_handler;
    callbacks.connection_close = websocket_close_handler;

    LOGD("start server");
    ctx = mg_start(&callbacks, NULL, options);

    LOGD("greeting");
    printf("%s started on port(s) %s with web root [%s]\n",
           server_name, mg_get_option(ctx, "listening_ports"),
           mg_get_option(ctx, "document_root"));

    change_state(RUNNING);
	LOGI("connect to fifo");

	FILE *fd;
	char file[256];
	char buf[MAX_BUFF_FIFO];
	sprintf(file, "%s/%s", mCacheDirectory, DAMN_SERVER_FILE);

	LOGI("fifo file path: %s", file);
	/* create the FIFO (named pipe) */
	umask(0);
	LOGI("umask done");
	mknod(file, S_IFIFO|0666, 0);

	LOGI("mknod done");
	//read from fifo

	fd = fopen(file, "r");
	LOGI("fopen done2");
	fgets(buf, MAX_BUFF_FIFO, fd);
	fclose(fd);

	LOGI("server closing");
	unlink(file);
	/* remove the FIFO */

	if(ctx!=NULL){
		mg_stop(ctx);
		ctx = NULL;
	}
	else
		LOGE("ctx == null"); //TODO not needed if ctx is static..

	change_state(OFF);
	LOGD("server terminated!!!!!!!");

    return;
}

bool stop_server(const char *cachedir)
{
	LOGD("stop server");

	char file[256];
	sprintf(file, "%s/%s", cachedir, DAMN_SERVER_FILE);

	FILE *fp;
	LOGI("fifo file: %s", file);
	if((fp = fopen(file, "w")) == NULL) {
		LOGE("fopen failed3");
		return false;
	}
	fputs("x", fp);
	fclose(fp);

	LOGI("stop server done");
	return true;
}

void Java_at_fhooe_mcm14_damn_jni_DAMNServer_startServerJNI(JNIEnv *env, jclass clazz, jstring docroot, jstring pemfile, jstring cachedir)
{
	UNUSED(clazz);

	const char *cDocroot = (*env)->GetStringUTFChars(env, docroot, 0);
	const char *cPemfile = (*env)->GetStringUTFChars(env, pemfile, 0);
	const char *cCachedir = (*env)->GetStringUTFChars(env, cachedir, 0);

	start_server(cDocroot, cPemfile, cCachedir);

	(*env)->ReleaseStringUTFChars(env, docroot, cDocroot);
	(*env)->ReleaseStringUTFChars(env, pemfile, cPemfile);
	(*env)->ReleaseStringUTFChars(env, cachedir, cCachedir);
}

void Java_at_fhooe_mcm14_damn_jni_DAMNServer2_startServerJNI(JNIEnv *env, jclass clazz, jstring docroot, jstring pemfile, jstring cachedir){
	Java_at_fhooe_mcm14_damn_jni_DAMNServer_startServerJNI(env, clazz, docroot, pemfile, cachedir);
}

bool Java_at_fhooe_mcm14_damn_jni_DAMNServer_stopServerJNI()//JNIEnv *env, jclass clazz)
{
	return stop_server(mCacheDirectory);
}

int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getConnectionsJNI()
{
	return 0; //TODO
}

int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getStateBlockingJNI()
{
	//block until something changed
	FILE *fd;
	char file[256];
	char buf[MAX_BUFF_FIFO];
	sprintf(file, "%s/%s", mCacheDirectory, NOTIFICATION_FILE);

	fd = fopen(file, "r");
//	LOGI("fopen done3");
	fgets(buf, MAX_BUFF_FIFO, fd);
	fclose(fd);

//	LOGI("return new state %d", atoi(buf));
	return atoi(buf);
}

int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getStateJNI()
{
	return state;
}

void Java_at_fhooe_mcm14_damn_jni_DAMNServer_pushJNI(JNIEnv *env, jclass clazz, int code, jstring message, jstring application, long long threadId)
{
	UNUSED(clazz);
	pthread_rwlock_wrlock(&rwlock);

	//send message
	const char *app = (*env)->GetStringUTFChars(env, application, 0);
	const char *cMessage = (*env)->GetStringUTFChars(env, message, 0);
	int size = strlen(cMessage) + 4; //3 digits for integer +1 for \0

	char payload[size];
	sprintf(payload, "%03d%s", code, cMessage);

	//send payload
	char file[256];
	char buf[size];

	sprintf(file, "/data/data/%s/%s_%s?%09llu", app, MSG_FILE, app, threadId);
	sprintf(buf, "%s%c", payload, '\0');


	FILE *fp;


	//read from fifo
	if(access(file, F_OK) == -1){
		LOGI("no push file");
		umask(0);
		mknod(file, S_IFIFO|0666, 0);
	}

	if((fp = fopen(file, "r+")) == NULL) {
		LOGE("open %s failed", file);
		pthread_rwlock_unlock(&rwlock);

		return;
	}
	fputs(buf, fp);
	fflush(fp);

	fclose(fp);

//	LOGI("pushed: %s to %s", buf, file);

	(*env)->ReleaseStringUTFChars(env, message, cMessage);
	(*env)->ReleaseStringUTFChars(env, application, app);

	pthread_rwlock_unlock(&rwlock);
}

jstring Java_at_fhooe_mcm14_damn_jni_DAMNServer_receiveBlockingJNI(JNIEnv *env, jclass clazz, jstring application, jlong threadId)
{
	UNUSED(clazz);

	//block until something changed
	const char *app = (*env)->GetStringUTFChars(env, application, 0);

	FILE *fd;
	char file[256];
	char buf[MAX_BUFF_FIFO];

//	LOGI("waiting for commands on: %s", app);
	sprintf(file, "/data/data/%s/%s_%s?%09llu", app, RECEIVE_FILE, app, threadId);

	//read from fifo
	if(access(file, F_OK) == -1){
		LOGI("no file - create it: %s", file);
		umask(0);
		mknod(file, S_IFIFO|0666, 0);
	}

	if((fd = fopen(file, "r")) == NULL) {
		LOGE("open failed [%s]", file);

		return NULL;
	}

	fgets(buf, MAX_BUFF_FIFO, fd);
	fclose(fd);

	(*env)->ReleaseStringUTFChars(env, application, app);
//	LOGD("receive: %s from [uid=%d euid=%d]",buf, getuid(), geteuid());

	return (*env)->NewStringUTF(env, buf);
}

void Java_at_fhooe_mcm14_damn_jni_DAMNServer_newAppJNI(JNIEnv *env, jclass clazz, jstring application, long long threadId)
{
	//send message
	const char *app = (*env)->GetStringUTFChars(env, application, 0);
//	LOGD("send new app: %s", app);

	//send payload
	char file[256];
	char buf[MAX_BUFF_FIFO];

	//delete file if exist
	sprintf(file, "/data/data/%s/%s_%s?%09llu", app, MSG_FILE, app, threadId);

	remove(file);

	sprintf(file, "/data/data/at.fhooe.mcm.faaaat/%s_at.fhooe.mcm.faaaat", MSG_FILE);
	sprintf(buf, "000%s?%09llu", app, threadId);

	FILE *fp;
	pthread_rwlock_wrlock(&rwlock);

	if((fp = fopen(file, "r+")) == NULL) {
		LOGE("open of main file failed");
		pthread_rwlock_unlock(&rwlock);

		return;
	}
	fputs(buf, fp);
	fclose(fp);
	pthread_rwlock_unlock(&rwlock);
//	LOGD("sent new app: %s", app);

	//wait for incoming connection of this thread..
	Java_at_fhooe_mcm14_damn_jni_DAMNServer_receiveBlockingJNI(env, clazz, application, threadId);

	(*env)->ReleaseStringUTFChars(env, application, app);
//	LOGD("BLOCK ENDs -> now send methods");
}


void Java_at_fhooe_mcm14_damn_jni_DAMNServer_deleteAppJNI(JNIEnv *env, jclass clazz, jstring application, long long threadId)
{
	UNUSED(clazz);

	//send message
	const char *app = (*env)->GetStringUTFChars(env, application, 0);
//	LOGD("send new app: %s", app);

	//send payload
	char file[256];

	//delete file if exist
	sprintf(file, "/data/data/%s/%s_%s?%09llu", app, MSG_FILE, app, threadId);
	remove(file);
	LOGD("delete file: %s", file);

	sprintf(file, "/data/data/%s/%s_%s?%09llu", app, RECEIVE_FILE, app, threadId);
	remove(file);
	LOGD("delete file: %s", file);

	(*env)->ReleaseStringUTFChars(env, application, app);
}
