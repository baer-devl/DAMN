package at.fhooe.mcm14.damn.misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class Extract {
	
	public static void unpackZipAsset(Context context, String resource, String path) {
		ZipInputStream zis = null;
		
		try {
	    	 zis = new ZipInputStream(new BufferedInputStream(context.getAssets().open(resource)));
	         ZipEntry ze;
	         int count;
	         byte[] buffer = new byte[8192];
	         
	         while ((ze = zis.getNextEntry()) != null) {
	             File file = new File(path, ze.getName());
	             File dir = ze.isDirectory() ? file : file.getParentFile();
	             Log.d("damn", "file: " + ze.getName());
	             
	             if (!dir.isDirectory() && !dir.mkdirs())
	                 throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
	             
	             if (ze.isDirectory())
	                 continue;
	             
	             FileOutputStream fout = new FileOutputStream(file);
	             
                 while ((count = zis.read(buffer)) != -1)
                     fout.write(buffer, 0, count);

                 fout.close();
                 Log.d("damn", "write done");
	         }
	         
	     } catch(Exception e)  {
	         Log.e("damn", e.getMessage());
	         
	     } finally {
	         try {zis.close();} catch (Exception e) { }
	     }

	     Log.d("damn", "done");
	}
	
	
	public static boolean unpackZip(Context context, String resource, String path) {
	     ZipInputStream zis;
	     
	     try 
	     {
	         String filename;
	         zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(resource)));          
	         ZipEntry ze;
	         byte[] buffer = new byte[1024];
	         int count;

	         while ((ze = zis.getNextEntry()) != null) 
	         {
	             // zapis do souboru
	             filename = ze.getName();

	             // Need to create directories if not exists, or
	             // it will generate an Exception...
	             if (ze.isDirectory()) {
	                File fmd = new File(path + filename);
	                fmd.mkdirs();
	                continue;
	             }

	             FileOutputStream fout = new FileOutputStream(path + filename);

	             // cteni zipu a zapis
	             while ((count = zis.read(buffer)) != -1) 
	             {
	                 fout.write(buffer, 0, count);             
	             }

	             fout.close();               
	             zis.closeEntry();
	         }

	         zis.close();
	     } 
	     catch(IOException e)
	     {
	         e.printStackTrace();
	         return false;
	     }

	    return true;
	}
	
	
    public static void copyAssetFolder(Context context, String assetFolder, String outputPath) {
        // "Name" is the name of your folder!
		AssetManager assetManager = context.getAssets();
		
	    try {
	        extractFiles(assetManager, assetFolder, assetManager.list(assetFolder), outputPath);
	        
	    } catch (IOException e) {
	        Log.e("ERROR", "Failed to get asset file list.", e);
	    }
	    
	}
    
    private static void extractFiles(AssetManager assetManager, String assetFolder, String[] files, String outputPath) {
    	// Analyzing all file on assets subfolder
    	Log.d("damn", "files: " + files.length);
	    for(String filename : files) {
	    	Log.d("damn", "create file: " + outputPath + "/" + filename);
	        InputStream in = null;
	        OutputStream out = null;
	        
	        File f = new File(outputPath, filename);
	        try {
	        	Log.d("damn", "asset file: " + assetFolder + "/" + filename);
	        	if(assetManager.list(assetFolder + "/" + filename).length!=0) {
		        	if(!f.exists()){
						f.mkdirs();
						f.setReadable(true, false);
		        		f.setWritable(true, false);
		            	f.setExecutable(true, false);
		        	}
		        	Log.d("damn", "in: " + assetFolder + "/" + filename + "->" + f.getAbsolutePath());
		        	extractFiles(assetManager, assetFolder + "/" + filename, assetManager.list(assetFolder + "/" + filename), f.getAbsolutePath());
		        }
	        	
	        	if(!f.exists()){
	        		f.createNewFile();
	        		f.setReadable(true, false);
	        		f.setWritable(true, false);
	        		
	        	}else
	        		continue;
	        	
		        Log.d("damn", "copy: " + assetFolder + "/" + filename + "->" + f.getAbsolutePath());
            	
                in = assetManager.open(assetFolder + "/" + filename);
                out = new FileOutputStream(f.getAbsolutePath());
                
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
                
            } catch(IOException e) {
                Log.e("ERROR", "Failed to copy asset file: " + filename, e);
            }
	    }
    }

	//Method used by copyAssets() on purpose to copy a file.
	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1) {
		    out.write(buffer, 0, read);
		}
	}
}
