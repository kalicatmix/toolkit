package toolkit.stream.impl;

import toolkit.stream.Input;
import toolkit.stream.Output;
import toolkit.stream.action.IAction;


public class DefaultAction implements IAction {
	public static interface Consumer{
	 public Output accept(Input input);
	}
	
	private Consumer consumer;
    public DefaultAction(Consumer  consumer) {
    	this.consumer = consumer;
	}
	@Override
	public Output next(Input input) {
		return next(input,consumer);
	}
    public Output next(Input input,Consumer consumer) {
    	return consumer.accept(input);
    }
}
