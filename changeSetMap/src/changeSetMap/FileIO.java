package changeSetMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

public class FileIO {
	File outPutFile;
	BufferedWriter bw;
	FileWriter fw;

    public FileIO (File file) {
	   outPutFile = file;
	   try {
		   		if (!file.exists()) file.createNewFile();
		   		fw = new FileWriter(file.getAbsoluteFile());
		   		bw = new BufferedWriter(fw);
	       } catch (IOException e) {
			  e.printStackTrace();
		   }
    }
    
    public void writeLinenToFile(String string) {
    	try {
			bw.write(string);
			bw.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void closeFile () {
    	try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
