package co.storyroll.exception;

public class APIException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 660249139002130236L;

	public APIException(String detailMessage) {
		super(detailMessage);
	}
}
