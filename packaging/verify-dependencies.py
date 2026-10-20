"""Compare cached, unmodified dependencies with their exact Maven Central releases."""
from pathlib import Path
import concurrent.futures, hashlib, urllib.request, urllib.error

root=Path(__file__).resolve().parent.parent
pairs=[]
for module in ['lwjgl','lwjgl-glfw','lwjgl-jemalloc','lwjgl-openal','lwjgl-opengl','lwjgl-stb','lwjgl-openvr']:
    for suffix in ['', '-natives-windows']:
        pairs.append((root/'lib'/(module+suffix+'.jar'), f'https://repo.maven.apache.org/maven2/org/lwjgl/{module}/3.3.3/{module}-3.3.3{suffix}.jar'))
pairs.extend([
    (root/'lib/gdx-backend-lwjgl3.jar','https://repo.maven.apache.org/maven2/com/badlogicgames/gdx/gdx-backend-lwjgl3/1.9.9/gdx-backend-lwjgl3-1.9.9.jar'),
    (root/'lib/javassist.jar','https://repo.maven.apache.org/maven2/org/javassist/javassist/3.30.2-GA/javassist-3.30.2-GA.jar')])
def check(pair):
    path,url=pair
    try:
        with urllib.request.urlopen(url+'.sha256',timeout=30) as response:expected=response.read().decode().strip().lower()
    except urllib.error.HTTPError as error:
        if error.code!=404:raise
        with urllib.request.urlopen(url,timeout=30) as response:expected=hashlib.sha256(response.read()).hexdigest()
    assert hashlib.sha256(path.read_bytes()).hexdigest()==expected, f'Modified dependency: {path.name}'
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    list(pool.map(check,pairs))
print(f'MAVEN HASH PASS: {len(pairs)} unmodified dependency JARs')
