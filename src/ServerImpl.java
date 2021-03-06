import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * @author Luiz Nunes Junior, Thiago Anders Imhoff 
 * @category Trabalho 1 de Interfaces e Periféricos
 * @since October 10th, 2016
 */

public class ServerImpl implements Server {
	static int port = 1515;
	static int timeout = 30000; // 30s
	static String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
	static ServerSocket server = null;
	static Socket client = null;
	static DataOutputStream out = null;
	static DataInputStream in = null;
	static ByteArrayOutputStream output = null;
	static byte arg1[] = null;
	static byte arg2[] = null;
	static byte file[] = null;
	static byte filesize[] = null;
	
	public static void main(String[] args) {
		for(;;){
		try {
			server = new ServerSocket(port);
			System.out.println("Waiting for a connection request...");
			client = server.accept();
			
			// the server will stop and wait for a successful connection
			// before proceeding
			
			in = new DataInputStream(client.getInputStream());
			out = new DataOutputStream(client.getOutputStream());
			
			// the server immediately send its current path to the client
			
			buildOutput( toByteArrayAlt(getCurrentPath()) );
			sendOutput();
			
			System.out.println("Connected.");
			
			for(;;) {
				// server only checks for an input if there's a connection
				byte command = in.readByte();
				System.out.println(command + " e o comando.");
				if(command == 8) {
					// close
					out.close();
					in.close();
					client.close();
					server.close();
				} else { execute(command); }
			}
		} catch (Exception e) {
			try{
			server.close();
			}
			catch(Exception ee){
			  ee.printStackTrace();
			  System.exit(1);    	
			}
			System.out.println("Cliente deu close. Reabrindo conexão...");
		}
		//} catch (Exception e) {
			//System.out.println("PROBLEMA INESPERADO: ");
			//e.printStackTrace();
			//System.exit(1);
		//}
		}
	}
	
	public static String getCurrentPath() {
		return currentPath;
	}
	
	private static void unpack1() {
		try {
			byte size1 = in.readByte();
			arg1 = new byte[size1];
			for(int iter = 0; iter < size1; iter++) { arg1[iter] = in.readByte(); }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void unpack2() {
		try {
			byte size1 = in.readByte();
			arg1 = new byte[size1];
			for(int iter = 0; iter < size1; iter++) { arg1[iter] = in.readByte(); }
			byte size2 = in.readByte();
			arg2 = new byte[size2];
			for(int iter = 0; iter < size2; iter++) { arg2[iter] = in.readByte(); }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void unpack3() {
		try {
			byte size1 = in.readByte();
			arg1 = new byte[size1];
			for(int iter = 0; iter < size1; iter++) { arg1[iter] = in.readByte(); }	
			byte filesizesize = in.readByte();
			filesize = new byte[filesizesize];
			for(int iter = 0; iter < filesizesize; iter++) { filesize[iter] = in.readByte(); }
			String temp = new String(filesize,"ASCII");
			int realfilesize = Integer.parseInt(temp);
			file = new byte[realfilesize];
			for(int iter = 0; iter < realfilesize; iter++) { file[iter] = in.readByte(); }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String toAsc2(byte transform[]) throws UnsupportedEncodingException {
		String answer = new String(transform, "ASCII");
		return answer;
	}
	
	public static void execute(byte command) throws UnsupportedEncodingException {
		if(command == 1) {
			ls();
		}
		else if(command == 2) {
			unpack1();
			cd(toAsc2(arg1));
		}
		else if(command == 3) {
			unpack2();
			mv(toAsc2(arg1), toAsc2(arg2));
		}
		else if(command == 4) {
			unpack1();
			mkdir(toAsc2(arg1));
		}
		else if(command == 5) {
			unpack1();
			rmdir(toAsc2(arg1));
		}
		else if(command == 6) {
			unpack1();
			rm(toAsc2(arg1));
		}
		else if(command == 7) {
			unpack2();
			cp(toAsc2(arg1), toAsc2(arg2));
		}
		else if(command == 9) {
			unpack1();
			cat(toAsc2(arg1));
		}
		else if(command == 10) {
			unpack3();
			upload(toAsc2(arg1), file);
		}
		else if(command == 11) {
			unpack1();
			download(toAsc2(arg1));
		}
		
		StreamDetector.eraseArguments();
	}
	
	// from string to a byte array where the first byte is the number of following bytes
	private static byte[] toByteArrayAlt(String string) {
		byte[] temp = string.getBytes();
		byte size = (byte) temp.length;
		byte[] destination = new byte[size + 1];
		destination[0] = size;
		System.arraycopy(temp,0,destination,1,size);
		//System.out.println("ack: " + string + "\n\tsize: " + size);
		return destination;
	}
	
	private static void operationStatus(boolean status) {
		byte[] array = new byte[1];
		
		if(status) { array[0] = (byte) 1; }
		else { array[0] = (byte) 0; }
		
		buildOutput(array);
	}
	
	private static void howManyOutputs(int size) {
		byte[] array = new byte[1];
		array[0] = (byte) size;
		buildOutput(array);
	}
	
	private static void buildOutput(byte[] array) {
		if(output == null) {
			output = new ByteArrayOutputStream();
		}
		
		try {
			output.write(array);
		} catch(Exception e) {
			System.out.println("Something went wrong.");
			e.printStackTrace();
		}
	}
	
	private static void sendOutput() {
		try {
			out.write(output.toByteArray());
			output = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean isAbsoluteDirectory(String path) {
		File verify = new File(path);
		
		if(verify.isDirectory() && verify.isAbsolute()) {
			return true;
		}
		
		return false;
	}
	
	private static boolean isAbsoluteFile(String path) {
		File verify = new File(path);
		
		if(verify.isFile() && verify.isAbsolute()) {
			return true;
		}
		
		return false;
	}
	
	//Childs tem todos os elementos no diretÃ³rio listados
	private static void ls() {
		File dir = new File(currentPath);
        String children[] = dir.list();
        
        try {
        	operationStatus(true);
    		howManyOutputs(children.length);
        	for(int iter = 0; iter < children.length; iter++) { buildOutput( toByteArrayAlt(children[iter]) ); }
        } catch (Exception e) {
        	operationStatus(false);
        	buildOutput( toByteArrayAlt("ls : something went wrong.") );
        }
        
        sendOutput();
	}
	
	//Atualizamos o currentPath
	private static void cd(String directory) {
		if(isAbsoluteDirectory(directory)) {
			currentPath = directory;
			operationStatus(true);
			buildOutput( toByteArrayAlt(getCurrentPath()) );
		} else if(directory.equals("..")) {
			File dir = new File(currentPath);
			String temp = dir.getParent();
			
			if(temp != null) {
				currentPath = temp;
			}
			
			operationStatus(true);
			buildOutput( toByteArrayAlt(getCurrentPath()) );
		} else {
			File dir = new File(currentPath + "/" + directory);
			
			if(dir.isDirectory() == true) {
				currentPath = currentPath + "/" + directory;
				operationStatus(true);
				buildOutput( toByteArrayAlt(getCurrentPath()) );
			} else {
				operationStatus(false);
	        	buildOutput( toByteArrayAlt("cd : no such file or directory.") );
			}
		}
		
		sendOutput();
	}
	
	//Tenta mover de source para target
	//Assumo que ela dÃ¡ os dois absolute path...
	//Se nÃ£o der tem que acrescentar currentPath
	private static void mv(String source, String target) {
		try {
			File src;
			
			if(isAbsoluteFile(source) || isAbsoluteDirectory(source)) {
				src = new File(source);
			} else { src = new File(currentPath + "/" + source); }
			
			String newdest;
			
			if(isAbsoluteDirectory(target)) {
				newdest = new String(target + "/" + src.getName());
			} else { newdest = new String(currentPath + "/" + target + "/" + src.getName()); }
			
			if( src.renameTo(new File(newdest)) ){
				operationStatus(true);
			} else {
				operationStatus(false);
	        	buildOutput( toByteArrayAlt("mv : something went wrong.") );
			}
		}
		catch(Exception e) {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("mv : something went wrong.") );
		}
		
		sendOutput();
	}
	
	//Criamos diretÃ³rio
	private static void mkdir(String directory) {
		File dir;
		
		File testing = new File(directory);
		
		if(testing.isAbsolute()) {
			dir = new File(directory); 
		}
		else { 
			dir = new File(currentPath + "/" + directory); 
		}
		
		if(dir.mkdir()) {
			operationStatus(true);
		}
		else {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("mkdir : something went wrong.") );
		}
		
		sendOutput();
	}
	
	//Para deletar pasta dentro de pasta
	private static void rmdir_aux(String directory) {
		File dir;
		
		if(isAbsoluteDirectory(directory)) { dir = new File(directory); }
		else { dir = new File(currentPath + "/" + directory); }
		
		for(File f: dir.listFiles()) {
			if(f.isDirectory()) rmdir_aux(dir + "/" + f.getName());
			else f.delete(); 
		}
		
		dir.delete();
	}
	
	//Destruimos o diretÃ³rio
	private static void rmdir(String directory) {
		File dir;
		
		if(isAbsoluteDirectory(directory)) { dir = new File(directory); }
		else { dir = new File(currentPath + "/" + directory); }
		
		for(File f: dir.listFiles()) {
			if(f.isDirectory()) rmdir_aux(dir + "/" + f.getName());
			else f.delete(); 
		}
		
		if(dir.delete()) {
			operationStatus(true);
		}
		else {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("rmdir : something went wrong.") );
		}

		sendOutput();
	}
	
	// delete files
	private static void rm(String file) {
		File dir;
		
		if(isAbsoluteFile(file)) { dir = new File(file); }
		else { dir = new File(currentPath + "/" + file); }
		
		if(dir.delete()) {
			operationStatus(true);
		}
		else {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("rm : something went wrong.") );
		}

		sendOutput();
	}
	
	// copy and paste files
	private static void cp(String source, String target) {
		try {
			
			File src;
			File tar;
			
			if(isAbsoluteFile(source)) { src = new File(source); }
			else { src = new File(currentPath + "/" + source); }
			
			if(isAbsoluteDirectory(target)) { tar = new File(target + "/" + src.getName()); }
			else { tar = new File(currentPath + "/" + target + "/" + src.getName()); }
			
			InputStream inStream = new FileInputStream(src);
	    	OutputStream outStream = new FileOutputStream(tar);
	    	
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	
	    	while( (length = inStream.read(buffer)) > 0) {
	    		outStream.write(buffer, 0, length);
	    	}
	    	
	    	inStream.close();
	    	outStream.close();
			
	    	operationStatus(true);
		}
		catch(Exception e) {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("cp : something went wrong.") );
		}

		sendOutput();
	}
	
	// needs to be remade
	/*private String[] close() {
		String message[] = {"[Success] Connection successfully closed."};
		try {
			socket.close();
		} catch (IOException e) {
			message[0] = "[Error] Connection shutdown has failed.";
		}
		return message;
	}*/
	
	private static void cat(String filename) {
		try {
			FileInputStream fis = null;

	        File src;
	        if(isAbsoluteFile(filename)) { src = new File(filename); }
			else { src = new File(currentPath + "/" + filename); }
	        
	        byte[] files = String.valueOf(src.length()).getBytes();
	        byte[] filess = new byte[1];
	        filess[0] = (byte) files.length;
	        
	        byte[] bsrc = new byte[(int) src.length()];
	        
            //convert file into array of bytes
        	fis = new FileInputStream(src);
        	fis.read(bsrc);
        	fis.close();
        	
        	String sscr = new String(bsrc, "ASCII");
		
        	operationStatus(true);
        	buildOutput( toByteArrayAlt(sscr) );
		} catch (Exception e) {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("cat : something went wrong.") );
		}
		
		sendOutput();
	}
	
	private static void upload(String filename, byte[] filex) {
		try {
			FileOutputStream fos;
			File testing = new File(filename);
			
			if(testing.isAbsolute()) { fos = new FileOutputStream(filename); }
			else { fos = new FileOutputStream(currentPath + "/" + filename); }
			
			fos.write(file);
			fos.close();
			
			operationStatus(true);
		} catch (Exception e) {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("upload : something went wrong.") );
		}
		
		sendOutput();
	}
	
	private static void download(String filename) {
		try {
			FileInputStream fis = null;

			File src;
			File testing = new File(filename);
			
	        if(testing.isAbsolute()) {
	        	src = new File(filename);
	        	//System.out.println(src.toString());
	        }
			else { 
				src = new File(currentPath + "/" + filename);
				//System.out.println(src.toString());
			}
	        
	        byte[] files = String.valueOf(src.length()).getBytes();
	        byte[] filess = new byte[1];
	        filess[0] = (byte) files.length;
	        
	        byte[] bsrc = new byte[(int) src.length()];
	        
            //convert file into array of bytes
        	fis = new FileInputStream(src);
        	fis.read(bsrc);
        	fis.close();
		
        	operationStatus(true);
        	buildOutput(filess);
        	buildOutput(files);
        	buildOutput(bsrc);
		} catch (Exception e) {
			operationStatus(false);
        	buildOutput( toByteArrayAlt("download : something went wrong.") );
		}
		
		sendOutput();
	}
}
