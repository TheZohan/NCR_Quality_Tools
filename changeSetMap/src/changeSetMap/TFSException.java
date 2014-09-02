package changeSetMap;

public class TFSException extends Exception{
	 
private static final long serialVersionUID = 1L;
private String msg = null;
public TFSException () {
	msg = "General TFS Connection error";
}
 public String getMessage(){
	 return this.msg;
 }
}
