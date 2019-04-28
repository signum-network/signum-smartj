package bt;

import java.lang.reflect.Method;

/**
 * A convenient 256 bit register (4 longs).
 * 
 * Used, for instance, when getting the hash of a block. This is also the current
 * size limit of messages received or sent by a contract.
 * 
 * @author jjos
 */
public class Register {
	public long value[] = new long[4];

	Method method;
	Object[] args;

	@EmulatorWarning
	public static Register newMethodCall(Method m, Object[] args){
		Register r = new Register();
		r.method = m;
		r.args = args;
		return r;
	}

	@EmulatorWarning
	public Method getMethod(){
		return method;
	}
	@EmulatorWarning
	public Object[] getMethodArgs(){
		return args;
	}

	@Override
	@EmulatorWarning
	public String toString() {
		if(method!=null){
			String ret = method.getName() + '(';
			for (int i = 0; i < args.length && args[i]!=null; i++) {
				if(i>0)
					ret += ", ";
				ret += args[i].toString();
			}
			ret += ')';
			return ret;
		}
		return super.toString();
	}
}
