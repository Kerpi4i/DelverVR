using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Windows.Forms;

internal static class Setup {
    const string Marker="DelverVR-managed-install-v1";
    static readonly string Profile=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"DelverVR");
    static string OwnExe { get { return Assembly.GetExecutingAssembly().Location; } }
    static string Quote(string s){return "\""+s.Replace("\"","\\\"")+"\"";}
    static string Inside(string root,string relative){
        string path=Path.GetFullPath(Path.Combine(root,relative));
        if(!path.StartsWith(Path.GetFullPath(root).TrimEnd('\\')+"\\",StringComparison.OrdinalIgnoreCase))throw new IOException("Invalid package path.");
        return path;
    }
    static void Log(string text){Directory.CreateDirectory(Profile);File.AppendAllText(Path.Combine(Profile,"installer.log"),DateTime.Now.ToString("s")+" "+text+Environment.NewLine);}
    static string Hash(string file){using(var s=File.OpenRead(file))using(var h=SHA256.Create())return BitConverter.ToString(h.ComputeHash(s)).Replace("-","");}
    static void Run(string exe,string arguments,string directory){
        var info=new ProcessStartInfo(exe,arguments){WorkingDirectory=directory,UseShellExecute=false,CreateNoWindow=true,RedirectStandardOutput=true,RedirectStandardError=true};
        using(var process=new Process()){process.StartInfo=info;process.OutputDataReceived+=(s,e)=>{if(e.Data!=null)lock(typeof(Setup))Log(e.Data);};process.ErrorDataReceived+=(s,e)=>{if(e.Data!=null)lock(typeof(Setup))Log(e.Data);};
            process.Start();process.BeginOutputReadLine();process.BeginErrorReadLine();process.WaitForExit();if(process.ExitCode!=0)throw new IOException("Local patch generation failed ("+process.ExitCode+"). See "+Path.Combine(Profile,"installer.log"));}
    }
    static void Extract(string target){
        using(var resource=Assembly.GetExecutingAssembly().GetManifestResourceStream("payload.zip"))
        using(var zip=new ZipArchive(resource,ZipArchiveMode.Read))foreach(var entry in zip.Entries){
            if(entry.FullName.EndsWith("/"))continue;string file=Inside(target,entry.FullName);
            Directory.CreateDirectory(Path.GetDirectoryName(file));using(var source=entry.Open())using(var destination=File.Create(file))source.CopyTo(destination);
        }
    }
    static void RemoveStage(string game,string stage){
        string prefix=Path.GetFullPath(game).TrimEnd('\\')+"\\DelverVR.installing-";
        if(Path.GetFullPath(stage).StartsWith(prefix,StringComparison.OrdinalIgnoreCase)&&Directory.Exists(stage))Directory.Delete(stage,true);
    }
    internal static string Install(string selected){
        string game=Path.GetFullPath(selected).TrimEnd('\\');
        string original=Path.Combine(game,"delver.jar"),java=Path.Combine(game,"jre-x64-windows","bin","java.exe");
        if(!File.Exists(original)||!File.Exists(java))throw new IOException("Choose the Delver game folder containing delver.jar and jre-x64-windows. A Windows x64 Steam installation is required.");
        string destination=Path.Combine(game,"DelverVR");
        if(Directory.Exists(destination)&&(!File.Exists(Path.Combine(destination,".delver-vr-install"))||File.ReadAllText(Path.Combine(destination,".delver-vr-install"))!=Marker))
            throw new IOException("An unrelated DelverVR folder already exists. Choose another location or rename that folder yourself.");
        string stage=Path.Combine(game,"DelverVR.installing-"+Guid.NewGuid().ToString("N"));
        try {
            Directory.CreateDirectory(stage);Extract(stage);
            string expected=File.ReadAllText(Path.Combine(stage,"supported-game.sha256")).Trim(),actual=Hash(original);
            if(!actual.Equals(expected,StringComparison.OrdinalIgnoreCase))throw new IOException("This release does not support your Delver build. No game files were changed. Installed game SHA256: "+actual);
            string generated=Path.Combine(stage,"generated"),mod=Path.Combine(stage,"mod.jar");
            Directory.CreateDirectory(generated);
            string cp=mod+";"+Path.Combine(stage,"lib","*")+";"+original;
            Run(java,"-cp "+Quote(cp)+" delver.vr.PatchGame "+Quote(original)+" "+Quote(mod)+" "+Quote(generated),stage);
            using(var zip=ZipFile.Open(Path.Combine(stage,"patch.jar"),ZipArchiveMode.Create))foreach(string file in Directory.GetFiles(generated,"*.class",SearchOption.AllDirectories)){
                string relative=file.Substring(generated.Length+1).Replace('\\','/');zip.CreateEntryFromFile(file,relative,CompressionLevel.Optimal);
            }
            if(!Directory.GetFiles(generated,"*.class",SearchOption.AllDirectories).Any())throw new IOException("No local patches were generated.");
            // Only this explicitly owned, freshly generated directory is removed.
            if(!Path.GetFullPath(generated).StartsWith(stage+"\\",StringComparison.OrdinalIgnoreCase))throw new IOException("Invalid staging directory.");
            Directory.Delete(generated,true);
            File.WriteAllText(Path.Combine(stage,"game.sha256"),actual);
            File.WriteAllText(Path.Combine(stage,".delver-vr-install"),Marker);
            File.WriteAllText(Path.Combine(stage,"version.txt"),"0.2.0-beta.4");
            var files=Directory.GetFiles(stage,"*",SearchOption.AllDirectories).Select(f=>f.Substring(stage.Length+1)).Concat(new[]{"installed-files.txt"}).Distinct().ToArray();
            File.WriteAllLines(Path.Combine(stage,"installed-files.txt"),files);
            if(Directory.Exists(destination)){
                string backup=Path.Combine(game,"DelverVR.backup-"+Guid.NewGuid().ToString("N"));
                Directory.Move(destination,backup);
                try{Directory.Move(stage,destination);}catch{Directory.Move(backup,destination);throw;}
                Log("Previous VR installation retained at "+backup);
            }else Directory.Move(stage,destination);
            Log("Installed "+destination+"; original game SHA256 "+actual);
            return Path.Combine(destination,"DelverVR.exe");
        }finally{RemoveStage(game,stage);}
    }
    static bool Managed(string root){return Directory.Exists(root)&&File.Exists(Path.Combine(root,".delver-vr-install"))&&File.ReadAllText(Path.Combine(root,".delver-vr-install"))==Marker&&File.Exists(Path.Combine(root,"installed-files.txt"));}
    static void RemoveInstall(string root,int parent,bool quiet){
        root=Path.GetFullPath(root).TrimEnd('\\');
        if(!Managed(root)||!Path.GetFileName(root).Equals("DelverVR",StringComparison.OrdinalIgnoreCase)||!File.Exists(Path.Combine(Directory.GetParent(root).FullName,"delver.jar")))throw new IOException("This is not a managed Delver VR installation.");
        if(parent>0)try{Process.GetProcessById(parent).WaitForExit(15000);}catch(ArgumentException){}
        string[] files=File.ReadAllLines(Path.Combine(root,"installed-files.txt"));
        foreach(string relative in files.Where(f=>f!=".delver-vr-install"&&f!="installed-files.txt")){string file=Inside(root,relative);if(File.Exists(file))File.Delete(file);}
        // Preserve recovery information until all managed binaries are removed.
        foreach(string directory in Directory.GetDirectories(root,"*",SearchOption.AllDirectories).OrderByDescending(d=>d.Length))if(!Directory.EnumerateFileSystemEntries(directory).Any())Directory.Delete(directory);
        bool untracked=Directory.EnumerateFileSystemEntries(root).Any(p=>Path.GetFileName(p)!=".delver-vr-install"&&Path.GetFileName(p)!="installed-files.txt");
        if(untracked){
            // Keep ownership metadata so a reinstall can safely recognize this folder.
            File.WriteAllLines(Path.Combine(root,"installed-files.txt"),new[]{".delver-vr-install","installed-files.txt"});
        }else{
            File.Delete(Path.Combine(root,".delver-vr-install"));File.Delete(Path.Combine(root,"installed-files.txt"));Directory.Delete(root);
        }
        Log("Uninstalled "+root+"; saves retained in "+Profile);
        if(!quiet)MessageBox.Show("Delver VR removed. Your original game and VR saves have been preserved.","Delver VR");
    }
    static void StartUninstall(bool quiet){
        string root=Path.GetDirectoryName(OwnExe);if(!Managed(root))throw new IOException("Uninstaller must be run from a managed DelverVR folder.");
        if(!quiet&&MessageBox.Show("Remove Delver VR? The original game and your VR saves will be kept.","Delver VR",MessageBoxButtons.YesNo,MessageBoxIcon.Question)!=DialogResult.Yes)return;
        string helper=Path.Combine(Path.GetTempPath(),"DelverVR-remove-"+Guid.NewGuid().ToString("N")+".exe");File.Copy(OwnExe,helper);
        bool elevate=false;string probe=Path.Combine(root,".write-test-"+Guid.NewGuid().ToString("N"));
        try{File.WriteAllText(probe,"");File.Delete(probe);}catch(UnauthorizedAccessException){elevate=true;}
        var info=new ProcessStartInfo(helper,"--remove "+Quote(root)+" "+Process.GetCurrentProcess().Id+(quiet?" /quiet":"")){UseShellExecute=true,WindowStyle=ProcessWindowStyle.Hidden};if(elevate)info.Verb="runas";
        Process.Start(info);
    }
    [STAThread] static int Main(string[] args){
        Application.EnableVisualStyles();bool quiet=args.Contains("/quiet");
        try{
            if(args.Length>=3&&args[0]=="--remove"){RemoveInstall(args[1],int.Parse(args[2]),quiet);return 0;}
#if UNINSTALL
            StartUninstall(quiet);return 0;
#else
            if(args.Length>=2&&args[0]=="/install"){
                string exe=Install(args[1]);if(!quiet)Success(exe);return 0;
            }
            Application.Run(new SetupForm());return 0;
#endif
        }catch(Exception error){Log(error.ToString());if(!quiet)MessageBox.Show(error.Message,"Delver VR setup",MessageBoxButtons.OK,MessageBoxIcon.Error);return 1;}
    }
    internal static void Success(string exe){MessageBox.Show("Installed successfully.\n\nStart VR directly:\n"+exe+"\n\nSteam: Games > Add a Non-Steam Game > Browse > select DelverVR.exe. Enable 'Include in VR Library' in its Steam properties.\n\nStart SteamVR before playing.\nSaves: "+Profile,"Delver VR",MessageBoxButtons.OK,MessageBoxIcon.Information);}
    sealed class SetupForm:Form {
        readonly TextBox path=new TextBox();readonly Button install=new Button();readonly Label status=new Label();bool working;
        public SetupForm(){
            Text="Delver VR Setup / Встановлення";Width=650;Height=290;FormBorderStyle=FormBorderStyle.FixedDialog;MaximizeBox=false;StartPosition=FormStartPosition.CenterScreen;
            Controls.Add(new Label{Left=20,Top=18,Width=590,Height=50,Text="Select your Delver game folder (contains delver.jar).\nВиберіть папку встановленої гри Delver. SteamVR and Touch controllers required."});
            path.SetBounds(20,78,490,26);path.Text=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86),"Steam","steamapps","common","Delver");Controls.Add(path);
            var browse=new Button{Left=520,Top=76,Width=95,Height=28,Text="Browse…"};browse.Click+=(s,e)=>{using(var picker=new FolderBrowserDialog()){picker.Description="Select Delver game folder";picker.SelectedPath=path.Text;picker.ShowNewFolderButton=false;if(picker.ShowDialog()==DialogResult.OK)path.Text=picker.SelectedPath;}};Controls.Add(browse);
            Controls.Add(new Label{Left=20,Top=120,Width=590,Height=42,Text="Installs into DelverVR inside the selected folder. Original game files stay unchanged.\nDirect VR start, no launcher window. Saves are stored in your Windows profile."});
            install.SetBounds(20,180,210,34);install.Text="Install / Встановити";Controls.Add(install);
            status.SetBounds(245,180,365,45);Controls.Add(status);
            install.Click+=(s,e)=>{
                working=true;install.Enabled=browse.Enabled=path.Enabled=false;status.Text="Preparing local VR patches…";string selected=path.Text;
                var worker=new BackgroundWorker();worker.DoWork+=(o,a)=>a.Result=Install(selected);
                worker.RunWorkerCompleted+=(o,a)=>{
                    working=false;install.Enabled=browse.Enabled=path.Enabled=true;
                    if(a.Error is UnauthorizedAccessException){
                        try{Process.Start(new ProcessStartInfo(OwnExe,"/install "+Quote(selected)){UseShellExecute=true,Verb="runas"});Close();}catch(Exception ex){status.Text=ex.Message;}
                    }else if(a.Error!=null){Log(a.Error.ToString());status.Text="Installation failed";MessageBox.Show(a.Error.Message,Text,MessageBoxButtons.OK,MessageBoxIcon.Error);}
                    else {status.Text="Installed";Success((string)a.Result);Close();}
                };worker.RunWorkerAsync();
            };
            FormClosing+=(s,e)=>{if(working)e.Cancel=true;};
        }
    }
}
