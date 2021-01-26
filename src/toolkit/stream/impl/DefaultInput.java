package toolkit.stream.impl;

import toolkit.stream.Input;
import toolkit.stream.Output;


public class DefaultInput implements Input{
private Object data;
public DefaultInput(Object data) {
	this.data=data;
}
@Override
public Object get() {
	return data;
}

@Override
public Output asOutput() {
	return new DefaultOutput(data);
}
}
