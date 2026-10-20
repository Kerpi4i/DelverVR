using System;
using System.Diagnostics;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Windows.Forms;

// A direct game entry point, with no launcher window. Steam tracks this process
// until the Java game exits. No PowerShell, JDK or external .NET download needed.
internal static class Start {
    internal static string Quote(string value) { return "\"" + value.Replace("\"", "\\\"") + "\""; }
    internal static string Hash(string file) {
        using(var s=File.OpenRead(file))using(var hash=SHA256.Create())return BitConverter.ToString(hash.ComputeHash(s)).Replace("-","");
    }
    [STAThread] static int Main(string[] args) {
        bool check=args.Length==1&&args[0]=="--check";
        bool smoke=args.Length==1&&args[0]=="--simulate-smoke";
        string profile=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"DelverVR");
        if(smoke)profile=Path.Combine(Path.GetTempPath(),"DelverVR-smoke");
        try {
            string root=AppDomain.CurrentDomain.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar);
            string game=Directory.GetParent(root).FullName;
            string jar=Path.Combine(game,"delver.jar"),java=Path.Combine(game,"jre-x64-windows","bin",smoke?"java.exe":"javaw.exe");
            if(!File.Exists(jar)||!File.Exists(java))throw new IOException("Delver VR must be installed in the Delver game folder. Run the installer again.");
            foreach(string name in new[]{"mod.jar","patch.jar","game.sha256",".delver-vr-install"})
                if(!File.Exists(Path.Combine(root,name)))throw new IOException("Incomplete VR installation: "+name+". Run the installer again.");
            if(!Hash(jar).Equals(File.ReadAllText(Path.Combine(root,"game.sha256")).Trim(),StringComparison.OrdinalIgnoreCase))
                throw new IOException("Delver has changed since the VR mod was installed. Install a compatible VR mod release before playing.");
            if(check)return 0;
            Directory.CreateDirectory(profile);
            string cp=Path.Combine(root,"patch.jar")+";"+Path.Combine(root,"mod.jar")+";"+Path.Combine(root,"lib","*")+";"+jar;
            string flags=smoke?" -Ddelver.vr.simulate=true -Ddelver.vr.smoke=true":"";
            var info=new ProcessStartInfo(java,"-Xmx2G"+flags+" -cp "+Quote(cp)+" delver.vr.Launcher");
            info.WorkingDirectory=profile;info.UseShellExecute=false;info.CreateNoWindow=true;
            info.RedirectStandardOutput=info.RedirectStandardError=true;
            using(var log=new StreamWriter(Path.Combine(profile,"delver-vr.log"),false,new UTF8Encoding(false)))
            using(var process=new Process()) {
                process.StartInfo=info;object gate=new object();
                DataReceivedEventHandler receive=(s,e)=>{if(e.Data!=null)lock(gate){log.WriteLine(e.Data);log.Flush();}};
                process.OutputDataReceived+=receive;process.ErrorDataReceived+=receive;
                process.Start();process.BeginOutputReadLine();process.BeginErrorReadLine();process.WaitForExit();
                if(process.ExitCode!=0)throw new IOException("VR game exited with code "+process.ExitCode+". See "+Path.Combine(profile,"delver-vr.log"));
            }
            return 0;
        } catch(Exception error) {
            Directory.CreateDirectory(profile);File.WriteAllText(Path.Combine(profile,"launch-error.txt"),error.ToString());
            if(!check&&!smoke)MessageBox.Show(error.Message,"Delver VR",MessageBoxButtons.OK,MessageBoxIcon.Error);
            return 1;
        }
    }
}
