package bt.compiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

public class FBCVisitor extends ClassVisitor {

    public FBCVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals(Compiler.TX_RECEIVED_METHOD) && descriptor.equals("()V"))
            return new TxReceivedAdapter(access, name, descriptor, signature, exceptions, mv);
        else
            return mv;
    }

    private static class TxReceivedAdapter extends MethodNode {
        private final MethodVisitor mv;

        public TxReceivedAdapter(int access, String name, String descriptor, String signature, String[] exceptions, MethodVisitor mv) {
            super(access, name, descriptor, signature, exceptions);
            this.mv = mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }
}
