# Third-party notices

Delver VR is an unofficial community modification, not endorsed by Priority
Interrupt, Valve or Meta. You need your own installed Steam copy of Delver.
The installer contains no Delver game archive, prepatched game classes, game
levels, music, sounds or commercial textures. Compatibility patches are
generated locally from the user's installation.

Unmodified runtime dependencies:

| Component | Version | License / source |
|---|---|---|
| LWJGL (core, GLFW, jemalloc, OpenAL, OpenGL, stb, OpenVR bindings) | 3.3.3 | BSD-3-Clause, https://github.com/LWJGL/lwjgl3/tree/3.3.3 |
| libGDX LWJGL3 backend | 1.9.9 | Apache-2.0, https://github.com/libgdx/libgdx/tree/1.9.9 |
| Javassist | 3.30.2-GA | Distributed under its Apache-2.0 option; Copyright Shigeru Chiba et al., https://github.com/jboss-javassist/javassist/tree/rel_3_30_2_ga |
| GLFW | 3.3.8 (LWJGL native dependency) | zlib/libpng, https://github.com/glfw/glfw/tree/3.3.8 |
| jemalloc | 5.3.0 (LWJGL native dependency) | BSD-style, https://github.com/jemalloc/jemalloc/tree/5.3.0 |
| OpenAL Soft | 1.23.1 (verified in bundled OpenAL.dll) | LGPL-2.0-or-later, https://github.com/kcat/openal-soft/tree/1.23.1 |
| OpenVR | LWJGL 3.3.3 native bindings | Valve BSD-style license, https://github.com/ValveSoftware/openvr |
| stb | LWJGL 3.3.3 native dependency | MIT or public-domain, https://github.com/nothings/stb |

License texts are in licenses/. OpenAL Soft 1.23.1 corresponding upstream
source and CMake build instructions are also supplied in
licenses/openal-soft-1.23.1-source.zip. This library is unmodified and loaded
dynamically from lib/lwjgl-openal-natives-windows.jar. You may replace that
library with a compatible rebuild; the mod's game compatibility hash check
does not prevent replacing dependency JARs. No third-party Java library has
been merged into our mod.jar. Java and Delver's libGDX core are used directly
from the user's existing game installation, not redistributed here.

Full dependency source archives are available from Maven Central using the
same coordinates and versions documented in bootstrap.ps1. Delver's separate
open-source engine license does not grant redistribution rights to its game
data: https://github.com/Interrupt/delverengine#delver-engine-open-source
