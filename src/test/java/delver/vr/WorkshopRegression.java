package delver.vr;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.interrupt.dungeoneer.game.ModManager;
public final class WorkshopRegression {
    static void write(Path path,String value)throws Exception{Files.createDirectories(path.getParent());Files.write(path,value.getBytes(StandardCharsets.UTF_8));}
    static String quote(Path p){return "\""+p.toString().replace("\\","\\\\")+"\"";}
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void run()throws Exception{
        Path root=Files.createTempDirectory(Paths.get("../../build").toAbsolutePath().normalize(),"workshop-fixture-");
        Path steam=root.resolve("Steam Library"),other=root.resolve("Other Library"),game=steam.resolve("steamapps/common/Delver");Files.createDirectories(game);
        Path local=game.resolve("mods/Local Content"),first=steam.resolve("steamapps/workshop/content/249630/10"),second=other.resolve("steamapps/workshop/content/249630/20");
        write(local.resolve("vr-fixture.txt"),"local");write(first.resolve("vr-fixture.txt"),"first");write(second.resolve("vr-fixture.txt"),"second");
        write(first.resolve("modInfo.json"),"{\"title\":\"VR Workshop fixture\",\"workshopId\":10}");
        write(steam.resolve("steamapps/workshop/content/249630/999/vr-fixture.txt"),"orphan");
        write(steam.resolve("steamapps/libraryfolders.vdf"),"// Test spaces, escaping, nested objects and duplicate libraries\n\"libraryfolders\" { \"0\" { \"path\" "+quote(steam)+" } \"1\" { \"path\" "+quote(other)+" } }");
        write(steam.resolve("steamapps/workshop/appworkshop_249630.acf"),"\"AppWorkshop\" { \"WorkshopItemsInstalled\" { \"10\" { \"manifest\" \"1\" } \"30\" { \"manifest\" \"1\" } } }");
        write(other.resolve("steamapps/workshop/appworkshop_249630.acf"),"\"AppWorkshop\" { \"WorkshopItemsInstalled\" { \"20\" { \"manifest\" \"2\" } } }");
        List<Path> found=Workshop.discover(game,steam);check(found.size()==3&&found.contains(first.toRealPath())&&found.contains(second.toRealPath())&&found.contains(local.toRealPath()),"Steam library discovery, deduplication or installed filtering failed: "+found);
        String oldGame=System.getProperty("delver.vr.gameDir"),oldSteam=System.getProperty("delver.vr.steamDir"),oldEnabled=System.getProperty("delver.vr.workshop");
        try{
            System.setProperty("delver.vr.gameDir",game.toString());System.setProperty("delver.vr.steamDir",steam.toString());System.setProperty("delver.vr.workshop","true");
            ModManager mods=new ModManager(true);
            check(mods.getAllMods().size==4,"Patched native ModManager did not see Workshop/local folders");
            check(mods.findFile("vr-fixture.txt").readString().equals("second"),"Native override resolution did not read installed content");
            check(mods.getModName(first.toString().replace('\\','/')).equals("VR Workshop fixture"),"Workshop title metadata not shown");
            mods.setEnabled(second.toString().replace('\\','/'),false);mods.refresh();
            check(mods.findFile("vr-fixture.txt").readString().equals("first"),"Native mod disabling/order failed");
            System.setProperty("delver.vr.workshop","false");ModManager disabled=new ModManager(true);check(disabled.getAllMods().size==1,"Workshop opt-out failed");
        }finally{restore("delver.vr.gameDir",oldGame);restore("delver.vr.steamDir",oldSteam);restore("delver.vr.workshop",oldEnabled);}
        testScroll();
        com.badlogic.gdx.Gdx.app.log("DelverVR","WORKSHOP PASS: installed manifests, multiple libraries, spaces/escapes, deduplication, missing/orphan filtering, native asset priority, names, enable/disable and VR menu scrolling");
    }
    static void testScroll(){
        com.badlogic.gdx.InputProcessor previous=com.badlogic.gdx.Gdx.input.getInputProcessor();Vr.Hand h=Vr.hands[1];
        com.badlogic.gdx.math.Matrix4 local=new com.badlogic.gdx.math.Matrix4(h.local);float stick=h.stickY,dt=Vr.dt;boolean valid=h.valid,menu=Vr.current.getShowingMenu();final int[] calls={0,0};
        try{
            if(!menu)Vr.pToggleInventory();NativeUi.update();h.valid=true;h.local.set(NativeUi.plane).translate(0,0,.2f);
            com.badlogic.gdx.Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter(){public boolean scrolled(int amount){calls[0]++;calls[1]=amount;return true;}});
            Vr.dt=.2f;h.stickY=-.9f;NativeUi.update();check(NativeUi.inside&&calls[0]==1&&calls[1]==1,"VR stick did not scroll menu down");
            Vr.dt=.01f;NativeUi.update();check(calls[0]==1,"Menu scroll repeat was not throttled");
            Vr.dt=.2f;h.stickY=.9f;NativeUi.update();check(calls[0]==2&&calls[1]==-1,"VR stick did not scroll menu up");
        }finally{com.badlogic.gdx.Gdx.input.setInputProcessor(previous);h.local.set(local);h.stickY=stick;h.valid=valid;Vr.dt=dt;if(!menu)Vr.pToggleInventory();NativeUi.update();}
    }
    static void restore(String key,String value){if(value==null)System.clearProperty(key);else System.setProperty(key,value);}
}
