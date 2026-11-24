// Join For More @PRIVATE_SRC_FILES
#include <jni.h>
#include <string>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <android/log.h>
#include <curl/curl.h>
#include <openssl/md5.h>
#include <json.hpp>
#include <obfuscate.h>
#include "oxorany.h"

#define LOG_TAG "ZtraxAuth"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using json = nlohmann::ordered_json;

time_t rng = 0;
static char AUTH_ENC[64] = {0};
static std::string exdate = oxorany("NULL");

std::string g_Token, g_Auth;
bool bValid = false;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_exdate(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(exdate.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_getAuthEnc(JNIEnv *env, jobject activityObject) {
    return env->NewStringUTF(AUTH_ENC);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_LogAct_GetKey(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(oxorany("https://t.me/PRIVATE_SRC_FILES"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_utils_Downtwo_Link(JNIEnv *env, jclass clazz) {
    std::string downloadUrl = oxorany("https://github.com/ztrax.zip");
    return env->NewStringUTF(downloadUrl.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_BoxApplication_getSdkKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(oxorany("PRIVATE_SRC_FILES"));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_bgmi_LogAct_nativeVerifySignature(JNIEnv *env, jobject thiz, jobject context) {
    return JNI_TRUE;
}

static std::string JStringToStdString(JNIEnv *env, jstring jstr) {
    if (!jstr) return "";
    const char *chars = env->GetStringUTFChars(jstr, nullptr);
    std::string out = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(jstr, chars);
    env->DeleteLocalRef(jstr);
    return out;
}

static std::string GetAndroidID(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getContentResolverMethod = env->GetMethodID(contextClass, "getContentResolver", "()Landroid/content/ContentResolver;");
    jclass settingSecureClass = env->FindClass("android/provider/Settings$Secure");
    jmethodID getStringMethod = env->GetStaticMethodID(settingSecureClass, "getString", "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;");

    jobject resolver = env->CallObjectMethod(context, getContentResolverMethod);
    jstring key = env->NewStringUTF("android_id");
    jstring str = (jstring) env->CallStaticObjectMethod(settingSecureClass, getStringMethod, resolver, key);
    env->DeleteLocalRef(key);
    if (resolver) env->DeleteLocalRef(resolver);
    env->DeleteLocalRef(contextClass);
    env->DeleteLocalRef(settingSecureClass);
    return JStringToStdString(env, str);
}

static std::string GetDeviceModel(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "MODEL", "Ljava/lang/String;");
    jstring str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    env->DeleteLocalRef(buildClass);
    return JStringToStdString(env, str);
}

static std::string GetDeviceBrand(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID brandId = env->GetStaticFieldID(buildClass, "BRAND", "Ljava/lang/String;");
    jstring str = (jstring) env->GetStaticObjectField(buildClass, brandId);
    env->DeleteLocalRef(buildClass);
    return JStringToStdString(env, str);
}

static std::string GetDeviceUniqueIdentifier(JNIEnv *env, const char *uuid) {
    jclass uuidClass = env->FindClass("java/util/UUID");
    size_t len = strlen(uuid);
    jbyteArray myJByteArray = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(myJByteArray, 0, (jsize) len, (const jbyte *) uuid);

    jmethodID nameUUIDFromBytesMethod = env->GetStaticMethodID(uuidClass, "nameUUIDFromBytes", "([B)Ljava/util/UUID;");
    jmethodID toStringMethod = env->GetMethodID(uuidClass, "toString", "()Ljava/lang/String;");

    jobject obj = env->CallStaticObjectMethod(uuidClass, nameUUIDFromBytesMethod, myJByteArray);
    jstring str = (jstring) env->CallObjectMethod(obj, toStringMethod);
    env->DeleteLocalRef(myJByteArray);
    if (obj) env->DeleteLocalRef(obj);
    env->DeleteLocalRef(uuidClass);
    return JStringToStdString(env, str);
}

struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *) userp;

    char *ptr = (char *) realloc(mem->memory, mem->size + realsize + 1);
    if (ptr == nullptr) {
        return 0;
    }
    mem->memory = ptr;
    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

static std::string CalcMD5(const std::string &s) {
    std::string result;
    unsigned char hash[MD5_DIGEST_LENGTH];
    char tmp[4];

    MD5_CTX md5;
    MD5_Init(&md5);
    MD5_Update(&md5, s.c_str(), s.length());
    MD5_Final(hash, &md5);
    for (unsigned char i : hash) {
        snprintf(tmp, sizeof(tmp), "%02x", i);
        result += tmp;
    }
    return result;
}

static std::string UrlEncode(const std::string &value) {
    std::string out;
    out.reserve(value.size());
    char buf[4];
    for (unsigned char c : value) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
            (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~') {
            out += (char) c;
        } else {
            snprintf(buf, sizeof(buf), "%%%02X", c);
            out += buf;
        }
    }
    return out;
}

static bool PostViaAndroidHttp(JNIEnv *env, jclass logActClass, const std::string &url,
                               const std::string &postBody, std::string &outBody, std::string &outErr) {
    if (!logActClass) return false;
    jmethodID mid = env->GetStaticMethodID(logActClass, "httpPostPanel",
                                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (env->ExceptionCheck() || !mid) {
        env->ExceptionClear();
        return false;
    }
    jstring jUrl = env->NewStringUTF(url.c_str());
    jstring jBody = env->NewStringUTF(postBody.c_str());
    jstring jResp = (jstring) env->CallStaticObjectMethod(logActClass, mid, jUrl, jBody);
    env->DeleteLocalRef(jUrl);
    env->DeleteLocalRef(jBody);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    std::string resp = JStringToStdString(env, jResp);
    if (resp.empty()) {
        outErr = "Empty response from server";
        return false;
    }
    if (resp.rfind("NET_ERR:", 0) == 0) {
        outErr = "Connection error: " + resp.substr(8);
        return false;
    }
    if (resp.rfind("HTTP_ERR:", 0) == 0) {
        outErr = "Server returned error (HTTP " + resp.substr(9) + ")";
        return false;
    }
    outBody = resp;
    return true;
}

static bool PostViaCurl(const std::string &url, const std::string &postBody,
                        std::string &outBody, std::string &outErr) {
    struct MemoryStruct chunk{};
    chunk.memory = (char *) malloc(1);
    if (!chunk.memory) {
        outErr = "Out of memory";
        return false;
    }
    chunk.memory[0] = '\0';
    chunk.size = 0;

    CURL *curl = curl_easy_init();
    if (!curl) {
        free(chunk.memory);
        outErr = "Failed to initialize network client";
        return false;
    }

    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_POST, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, postBody.c_str());
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (long) postBody.size());
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTREDIR, CURL_REDIR_POST_ALL);
    curl_easy_setopt(curl, CURLOPT_HTTP_VERSION, CURL_HTTP_VERSION_1_1);

    struct curl_slist *headers = nullptr;
    headers = curl_slist_append(headers, "Accept: application/json");
    headers = curl_slist_append(headers, "Content-Type: application/x-www-form-urlencoded");
    headers = curl_slist_append(headers, "Charset: UTF-8");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &chunk);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYSTATUS, 0L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "Mozilla/5.0 (Linux; Android 13; Mobile)");
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 15L);
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 10L);

    CURLcode res = curl_easy_perform(curl);
    if (headers) {
        curl_slist_free_all(headers);
    }

    bool ok = false;
    if (res == CURLE_OK) {
        long http_code = 0;
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);
        if (chunk.size > 0 && chunk.memory != nullptr) {
            outBody.assign(chunk.memory, chunk.size);
            ok = true;
        } else if (http_code != 200) {
            outErr = "Server returned error (HTTP " + std::to_string(http_code) + ")";
        } else {
            outErr = "Empty response from server";
        }
    } else {
        outErr = std::string("Connection error: ") + curl_easy_strerror(res);
    }

    curl_easy_cleanup(curl);
    free(chunk.memory);
    return ok;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_LogAct_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    bValid = false;
    g_Token.clear();
    g_Auth.clear();
    AUTH_ENC[0] = '\0';

    if (!mUserKey) {
        return env->NewStringUTF("Please enter key");
    }

    const char *rawKey = env->GetStringUTFChars(mUserKey, nullptr);
    std::string userKey = rawKey ? rawKey : "";
    if (rawKey) env->ReleaseStringUTFChars(mUserKey, rawKey);

    if (userKey.empty()) {
        return env->NewStringUTF("Please enter key");
    }

    std::string hwid = userKey;
    hwid += GetAndroidID(env, mContext);
    hwid += GetDeviceModel(env);
    hwid += GetDeviceBrand(env);
    std::string UUID = GetDeviceUniqueIdentifier(env, hwid.c_str());

    std::string panelUrl = oxorany("https://connect"); // Kuro Panel Link Here

    std::string postData = "game=PUBG&user_key=" + UrlEncode(userKey) + "&serial=" + UrlEncode(UUID);

    std::string responseBody;
    std::string errMsg = "Unknown error";

    bool fetched = PostViaAndroidHttp(env, clazz, panelUrl, postData, responseBody, errMsg);
    if (!fetched) {
        std::string curlErr;
        fetched = PostViaCurl(panelUrl, postData, responseBody, curlErr);
        if (!fetched && !curlErr.empty()) {
            errMsg = curlErr;
        }
    }

    if (fetched) {
        size_t jsonStart = responseBody.find('{');
        if (jsonStart == std::string::npos) {
            errMsg = "Invalid server response (non-JSON)";
        } else {
            try {
                json result = json::parse(responseBody.substr(jsonStart));
                bool is_success = false;
                if (result.contains("status")) {
                    if (result["status"].is_boolean()) {
                        is_success = result["status"].get<bool>();
                    } else if (result["status"].is_string()) {
                        std::string s = result["status"].get<std::string>();
                        is_success = (s == "true" || s == "1" || s == "success" || s == "ok");
                    } else if (result["status"].is_number()) {
                        is_success = (result["status"].get<int>() == 1);
                    }
                }

                if (is_success) {
                    std::string token;
                    std::string encVal;
                    bool hasData = false;

                    if (result.contains("data") && result["data"].is_object()) {
                        hasData = true;
                        auto &d = result["data"];
                        if (d.contains("token") && d["token"].is_string()) {
                            token = d["token"].get<std::string>();
                        }
                        if (d.contains("Enc") && d["Enc"].is_string()) {
                            encVal = d["Enc"].get<std::string>();
                        }
                        if (d.contains("EXP") && d["EXP"].is_string()) {
                            exdate = d["EXP"].get<std::string>();
                        } else if (d.contains("exp") && d["exp"].is_string()) {
                            exdate = d["exp"].get<std::string>();
                        }
                        if (d.contains("rng")) {
                            if (d["rng"].is_number()) {
                                rng = d["rng"].get<time_t>();
                            } else if (d["rng"].is_string()) {
                                try {
                                    rng = std::stol(d["rng"].get<std::string>());
                                } catch (...) {}
                            }
                        }
                    } else {
                        if (result.contains("token") && result["token"].is_string()) {
                            token = result["token"].get<std::string>();
                            hasData = true;
                        }
                        if (result.contains("EXP") && result["EXP"].is_string()) {
                            exdate = result["EXP"].get<std::string>();
                            hasData = true;
                        }
                    }

                    std::string auth = "PUBG-" + userKey + "-" + UUID + "-";
                    std::string license = oxorany("Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E");
                    auth += license;
                    std::string expectedMd5 = CalcMD5(auth);

                    g_Auth = expectedMd5;
                    g_Token = !token.empty() ? token : expectedMd5;

                    if (token == expectedMd5 || !token.empty() || hasData) {
                        bValid = true;
                        if (!encVal.empty()) {
                            strncpy(AUTH_ENC, encVal.c_str(), sizeof(AUTH_ENC) - 1);
                            AUTH_ENC[sizeof(AUTH_ENC) - 1] = '\0';
                        }
                    } else {
                        errMsg = "Authentication failed (invalid panel data)";
                    }
                } else {
                    if (result.contains("reason") && result["reason"].is_string()) {
                        errMsg = result["reason"].get<std::string>();
                    } else if (result.contains("message") && result["message"].is_string()) {
                        errMsg = result["message"].get<std::string>();
                    } else if (result.contains("msg") && result["msg"].is_string()) {
                        errMsg = result["msg"].get<std::string>();
                    } else {
                        errMsg = "Key not registered or expired";
                    }
                }
            } catch (const json::exception &e) {
                errMsg = std::string("JSON error: ") + e.what();
            }
        }
    }

    return bValid ? env->NewStringUTF("OK") : env->NewStringUTF(errMsg.c_str());
}
