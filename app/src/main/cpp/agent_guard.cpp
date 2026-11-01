#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <android/log.h>

#define LOG_TAG "AgentGuard"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_prootagent_app_core_NativeGuard_isProcessAlive(JNIEnv *env, jobject /* this */, jint pid) {
    if (pid <= 0) return JNI_FALSE;
    if (kill(pid, 0) == 0) {
        LOGI("Process %d is alive.", pid);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_prootagent_app_core_NativeGuard_getEnvironmentString(JNIEnv *env, jobject /* this */) {
    std::string envStr = "arm64-v8a;Linux;Proot";
    return env->NewStringUTF(envStr.c_str());
}

}
