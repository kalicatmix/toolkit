package toolkit.stream;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import toolkit.stream.action.IAction;


public class PipeLine {
private final LinkedList<IAction> actions = new LinkedList<IAction>();
private Input  input;
private Output output;
private PipeLine() {}
public static PipeLine in(IAction...actions) {
	PipeLine pipeLine = new PipeLine();
	pipeLine.actions.addAll(Arrays.asList(actions));
	pipeLine.pipeAll();
	return pipeLine;
}
private PipeLine pipeAll() {
	actions.forEach((action)->{
		output=action.next(input);
		input=output.asInput();
	});
	return this;
}
public PipeLine next(IAction action) {
	output=action.next(input);
	input=output.asInput();
	return this;
}

public PipeLine next(List<IAction> actions) {
	 actions.forEach((action)->{
		output=action.next(input);
		input=output.asInput();
	});
	return this;
}
public Output out() {
	return output;
}
}
