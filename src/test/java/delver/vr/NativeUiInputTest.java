package delver.vr;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.interrupt.dungeoneer.GameInput;
import java.lang.reflect.Proxy;
public final class NativeUiInputTest {
    public static void main(String[] args){
        // Android: setCursorCatched is a no-op and isCursorCatched always false.
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(p,m,a)->{
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==int.class)return 0;
            return null;
        });
        GameInput input=new GameInput();
        input.setCursorCatched(false);
        if(input.caughtCursor)throw new AssertionError("Inventory mouse hover/drag remains blocked on Android");
        input.setCursorCatched(true);
        if(!input.caughtCursor)throw new AssertionError("Gameplay cursor state not restored");
        input.setCursorCatched(false);
        if(input.caughtCursor)throw new AssertionError("Reopening inventory remains blocked");
        System.out.println("Android inventory cursor state PASS");
    }
}
