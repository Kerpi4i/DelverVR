package delver.vr;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.game.Game;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** Read Steam's local installed-item manifests; Steam remains responsible for downloads. */
public final class Workshop {
    static final String APP="249630";
    static Path gameDirectory(){
        String configured=System.getProperty("delver.vr.gameDir");if(configured!=null)return Paths.get(configured).toAbsolutePath().normalize();
        try{return Paths.get(Game.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().getParent();}catch(Exception e){return null;}
    }
    public static void append(Array<String> all){
        if(!Boolean.parseBoolean(System.getProperty("delver.vr.workshop","true"))||Gdx.app.getType()==Application.ApplicationType.Android)return;
        Path game=gameDirectory();String steam=System.getProperty("delver.vr.steamDir");
        for(Path folder:discover(game,steam==null?null:Paths.get(steam))){String path=folder.toString().replace('\\','/');boolean duplicate=false;
            for(String existing:all)if(!existing.equals("."))try{if(Paths.get(existing).toRealPath().equals(folder)){duplicate=true;break;}}catch(IOException ignored){}
            if(!duplicate){all.add(path);Gdx.app.log("DelverVR","Content mod: "+path);}
        }
    }
    static List<Path> discover(Path game,Path steam){
        LinkedHashSet<Path> result=new LinkedHashSet<Path>(),libraries=new LinkedHashSet<Path>();
        if(game!=null){addDirectories(game.resolve("mods"),result);for(Path p=game;p!=null;p=p.getParent())if(p.getFileName()!=null&&p.getFileName().toString().equalsIgnoreCase("steamapps")){libraries.add(p);break;}}
        if(steam!=null)libraries.add(steam.resolve("steamapps"));
        for(Path apps:new ArrayList<Path>(libraries)){
            Path file=apps.resolve("libraryfolders.vdf");Map<String,Object> folders=child(read(file),"libraryfolders");
            for(Object value:folders.values())if(value instanceof Map){Object path=((Map<?,?>)value).get("path");if(path instanceof String)libraries.add(Paths.get((String)path).resolve("steamapps"));}
            // Older Steam clients use numeric key -> library path instead of nested records.
            for(Map.Entry<String,Object> e:folders.entrySet())if(e.getKey().matches("[0-9]+")&&e.getValue() instanceof String)libraries.add(Paths.get((String)e.getValue()).resolve("steamapps"));
        }
        for(Path apps:libraries){Map<String,Object> installed=child(child(read(apps.resolve("workshop/appworkshop_"+APP+".acf")),"AppWorkshop"),"WorkshopItemsInstalled");
            for(String id:installed.keySet())if(id.matches("[0-9]+")){Path mod=apps.resolve("workshop/content/"+APP+"/"+id);if(Files.isDirectory(mod))add(mod,result);}
        }
        return new ArrayList<Path>(result);
    }
    private static void addDirectories(Path directory,Set<Path> result){if(!Files.isDirectory(directory))return;try(java.nio.file.DirectoryStream<Path> stream=Files.newDirectoryStream(directory)){List<Path> sorted=new ArrayList<Path>();for(Path p:stream)if(Files.isDirectory(p))sorted.add(p);Collections.sort(sorted);for(Path p:sorted)add(p,result);}catch(IOException e){warn(directory,e);}}
    private static void add(Path p,Set<Path> result){try{result.add(p.toRealPath());}catch(IOException e){warn(p,e);}}
    @SuppressWarnings("unchecked") private static Map<String,Object> child(Map<String,Object> parent,String key){Object value=parent.get(key);return value instanceof Map?(Map<String,Object>)value:Collections.<String,Object>emptyMap();}
    private static Map<String,Object> read(Path file){if(!Files.isRegularFile(file))return Collections.emptyMap();try{if(Files.size(file)>8*1024*1024)throw new IOException("Manifest too large");return new Vdf(new String(Files.readAllBytes(file),StandardCharsets.UTF_8)).map(false);}catch(Exception e){warn(file,e);return Collections.emptyMap();}}
    private static void warn(Path p,Exception e){if(Gdx.app!=null)Gdx.app.error("DelverVR","Cannot read mod manifest "+p+": "+e.getMessage());}
    /** Valve KeyValues: quoted/unquoted tokens, escapes, comments and nested objects. */
    static final class Vdf {
        final String text;int pos;
        Vdf(String text){this.text=text;}
        String token(){while(pos<text.length()){char c=text.charAt(pos);if(Character.isWhitespace(c)||c=='\uFEFF'){pos++;continue;}if(c=='/'&&pos+1<text.length()&&text.charAt(pos+1)=='/'){while(pos<text.length()&&text.charAt(pos)!='\n')pos++;continue;}break;}if(pos==text.length())return null;
            char c=text.charAt(pos++);if(c=='{'||c=='}')return String.valueOf(c);StringBuilder b=new StringBuilder();
            if(c=='"'){while(pos<text.length()){c=text.charAt(pos++);if(c=='"')return b.toString();if(c=='\\'&&pos<text.length()){char next=text.charAt(pos);if(next=='\\'||next=='"'){b.append(next);pos++;continue;}}b.append(c);}throw new IllegalArgumentException("Unterminated string");}
            b.append(c);while(pos<text.length()&&!Character.isWhitespace(text.charAt(pos))&&text.charAt(pos)!='{'&&text.charAt(pos)!='}')b.append(text.charAt(pos++));return b.toString();
        }
        Map<String,Object> map(boolean nested){Map<String,Object> out=new LinkedHashMap<String,Object>();String key;while((key=token())!=null){if(key.equals("}")){if(!nested)throw new IllegalArgumentException("Unexpected closing brace");return out;}String value=token();if(value==null||value.equals("}"))throw new IllegalArgumentException("Missing value");out.put(key,value.equals("{")?map(true):value);}if(nested)throw new IllegalArgumentException("Missing closing brace");return out;}
    }
}
