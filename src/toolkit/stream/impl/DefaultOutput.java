package toolkit.stream.impl;

import toolkit.stream.Input;
import toolkit.stream.Output;


public class DefaultOutput implements Output{
private Object data;
public DefaultOutput(Object data) {
	this.data=data;
}
@Override
public Object get() {
	return data;
}
@Override
public Input asInput() {
	return new DefaultInput(data);
}
}
