After the 10.4.0 release, RCE's code repository was split into multiple repositories,
most importantly the "rce-main" repository (the one containing this file), and the
"rce-thirdparty" repository. The latter contains the build definitions and
binaries for the "RCE third-party artifact repository", sometimes simply referred to
as the "RCE target platform" following common Eclipse terminology.

Most developers do not need to concern themselves with the third-party repository,
as using the standard .target files in the "remote" subdirectory is sufficient.

For developers who add, upgrade, or remove third-party artifacts, however, setting
a local build of the third-party repository (typically including uncommitted changes)
is necessary to validate or adapt the code in "rce-main" against these changes.

Due to the repository split, referencing such a local build either requires entering
the local path to the third-party repository build output, or following a convention.
The "use_local_platform_build.target" file supports the latter: If you check out the
"rce-thirdparty" next to the "rce-main" repository in your local filesystem, and
then build the "rce-thirdparty" repository, this .target file should allow you to
set the resulting build as your Eclipse target platform.

(Note that the relative paths in the third-party repository may undergo some changes
in the near future. For this reason, the .target file cross-reference will only work
if the checked out version of the third-party repository matches the rce-main 
repository's checked out version.)
