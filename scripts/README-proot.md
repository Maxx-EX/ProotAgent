# Proot 二进制放置说明

CI 不联网下载 proot，避免源失效导致构建失败。

在 Termux 中提取（arm64）：

    pkg install -y proot
    cp $PREFIX/bin/proot ~/ProotAgent/app/src/main/jniLibs/arm64-v8a/libproot.so
    chmod 644 ~/ProotAgent/app/src/main/jniLibs/arm64-v8a/libproot.so

要点：
- 文件名必须是 lib*.so，AGP 才会打进 nativeLibraryDir。
- app/build.gradle.kts 已设 jniLibs.useLegacyPackaging = true，安装后解压到只读可执行目录，绕过 Android 10+ W^X。
- rootfs（debian/ubuntu tar.gz）不进 APK，首次运行时由用户配置的源下载到 SD 卡。
