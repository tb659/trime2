// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

#include <rime_api.h>

#include <memory>
#include <string>
#include <vector>

#include <rime/dict/dictionary.h>
#include <rime/dict/user_dictionary.h>
#include <rime/candidate.h>
#include <rime/context.h>
#include <rime/menu.h>
#include <rime/service.h>
#include <rime/schema.h>
#include <rime/ticket.h>

#include "jni-utils.h"
#include "objconv.h"
#include "proto.h"
#include "session.h"

#define MAX_BUFFER_LENGTH 2048

extern void rime_require_module_lua();
extern void rime_require_module_octagram();
extern void rime_require_module_predict();
// librime is compiled as a static library, we have to link modules explicitly
static void declare_librime_module_dependencies() {
  rime_require_module_lua();
  rime_require_module_octagram();
  rime_require_module_predict();
}

namespace {

bool isSelfCreatedCandidate(const rime::an<rime::Candidate>& candidate) {
  auto genuine = rime::Candidate::GetGenuineCandidate(candidate);
  if (!genuine) {
    return false;
  }
  const auto& type = genuine->type();
  return type == "user_phrase" || type == "user_table";
}

}  // namespace

class Rime {
 public:
  Rime() : rime(rime_get_api()) {}
  Rime(Rime const &) = delete;
  void operator=(Rime const &) = delete;

  static Rime &Instance() {
    static Rime instance;
    return instance;
  }

  void startup(bool fullCheck,
               const RimeNotificationHandler &notificationHandler) {
    if (!rime) return;
    const char *userDir = getenv("RIME_USER_DATA_DIR");
    const char *sharedDir = getenv("RIME_SHARED_DATA_DIR");
    const char *versionName = getenv("RIME_DISTRIBUTION_VERSION");

    RIME_STRUCT(RimeTraits, trime_traits)
    trime_traits.shared_data_dir = sharedDir;
    trime_traits.user_data_dir = userDir;
    trime_traits.log_dir = "";  // set empty log_dir to log to logcat only
    trime_traits.app_name = "rime.trime";
    trime_traits.distribution_name = "Trime";
    trime_traits.distribution_code_name = "trime";
    trime_traits.distribution_version = versionName;

    rime->setup(&trime_traits);
    rime->initialize(&trime_traits);
    rime->set_notification_handler(notificationHandler, GlobalRef->jvm);
    rime->start_maintenance(fullCheck);
  }

  bool deploySchema(std::string_view schemaFile) {
    return rime->deploy_schema(schemaFile.data());
  }

  bool deployConfigFile(std::string_view configFile,
                        std::string_view versionKey) {
    return rime->deploy_config_file(configFile.data(), versionKey.data());
  }

  bool processKey(int keycode, int mask) {
    return rime->process_key(session(), keycode, mask);
  }

  bool simulateKeySequence(const std::string &sequence) {
    return rime->simulate_key_sequence(session(), sequence.data());
  }

  bool commitComposition() { return rime->commit_composition(session()); }

  void clearComposition() { rime->clear_composition(session()); }

  void commitProto(uintptr_t builder) { rime_commit_proto(session(), builder); }

  void contextProto(uintptr_t builder) {
    rime_context_proto(session(), builder);
  }

  void statusProto(uintptr_t builder) { rime_status_proto(session(), builder); }

  void setOption(std::string_view key, bool value) {
    rime->set_option(session(), key.data(), value);
  }

  bool getOption(std::string_view key) {
    return rime->get_option(session(), key.data());
  }

  std::string currentSchemaId() {
    char result[MAX_BUFFER_LENGTH];
    return rime->get_current_schema(session(), result, MAX_BUFFER_LENGTH)
               ? result
               : "";
  }

  std::vector<SchemaItem> schemaList() {
    std::vector<SchemaItem> result;
    RimeSchemaList list{};
    if (rime->get_schema_list(&list)) {
      result = SchemaItem::fromCList(list);
      rime->free_schema_list(&list);
    }
    return std::move(result);
  }

  bool selectSchema(std::string_view schemaId) {
    return rime->select_schema(session(), schemaId.data());
  }

  std::string rawInput() {
    auto cStr = rime->get_input(session());
    return cStr ? cStr : "";
  }

  bool learnRawInput(std::string_view text) {
    if (text.empty()) {
      return false;
    }
    std::string schema_id = currentSchemaId();
    if (schema_id.empty() || schema_id == ".default") {
      return false;
    }

    rime::Schema schema(schema_id);
    if (!schema.config()) {
      return false;
    }
    rime::Ticket ticket(&schema, "translator");

    auto user_dictionary = rime::UserDictionary::Require("user_dictionary");
    if (!user_dictionary) {
      return false;
    }
    std::unique_ptr<rime::UserDictionary> user_dict(user_dictionary->Create(ticket));
    if (!user_dict || !user_dict->Load() || user_dict->readonly()) {
      return false;
    }

    if (auto dictionary = rime::Dictionary::Require("dictionary")) {
      std::unique_ptr<rime::Dictionary> dict(dictionary->Create(ticket));
      if (dict && dict->Load()) {
        user_dict->Attach(dict->primary_table(), dict->prism());
      }
    }

    rime::DictEntry entry;
    entry.text = std::string(text);
    entry.custom_code = std::string(text);
    return user_dict->UpdateEntry(entry, 1);
  }

  std::vector<std::string> queryRawInputCompletions(std::string_view prefix,
                                                    size_t limit) {
    std::vector<std::string> result;
    if (prefix.empty() || limit == 0) {
      return result;
    }

    std::string schema_id = currentSchemaId();
    if (schema_id.empty() || schema_id == ".default") {
      return result;
    }

    rime::Schema schema(schema_id);
    if (!schema.config()) {
      return result;
    }
    rime::Ticket ticket(&schema, "translator");

    auto user_dictionary = rime::UserDictionary::Require("user_dictionary");
    if (!user_dictionary) {
      return result;
    }
    std::unique_ptr<rime::UserDictionary> user_dict(user_dictionary->Create(ticket));
    if (!user_dict || !user_dict->Load()) {
      return result;
    }

    if (auto dictionary = rime::Dictionary::Require("dictionary")) {
      std::unique_ptr<rime::Dictionary> dict(dictionary->Create(ticket));
      if (dict && dict->Load()) {
        user_dict->Attach(dict->primary_table(), dict->prism());
      }
    }

    rime::UserDictEntryIterator iter;
    user_dict->LookupWords(&iter, std::string(prefix), true, limit, nullptr);
    // mixed completion 走的是 Java 侧补候选链，这里直接按用户实际提交次数排序，
    // 再用 Rime 已计算出的 weight 兜底，确保 tb659 / tb56 这类词能稳定动态调频。
    struct WeightedCompletionEntry {
      std::string text;
      int commit_count;
      double weight;
    };
    std::vector<WeightedCompletionEntry> weighted_entries;
    for (auto entry = iter.Peek(); entry; entry = iter.Next() ? iter.Peek() : nullptr) {
      if (!entry || entry->text.empty()) {
        continue;
      }
      weighted_entries.push_back(
          {entry->text, entry->commit_count, entry->weight});
    }
    std::stable_sort(
        weighted_entries.begin(), weighted_entries.end(),
        [](const auto& lhs, const auto& rhs) {
          if (lhs.commit_count != rhs.commit_count) {
            return lhs.commit_count > rhs.commit_count;
          }
          if (lhs.weight != rhs.weight) {
            return lhs.weight > rhs.weight;
          }
          return lhs.text < rhs.text;
        });
    for (const auto& item : weighted_entries) {
      result.emplace_back(item.text);
    }
    return result;
  }

  size_t caretPosition() { return rime->get_caret_pos(session()); }

  void setCaretPosition(size_t caretPos) {
    rime->set_caret_pos(session(), caretPos);
  }

  bool selectCandidateOnCurrentPage(size_t index) {
    return rime->select_candidate_on_current_page(session(), index);
  }

  bool deleteCandidateOnCurrentPage(size_t index) {
    return rime->delete_candidate_on_current_page(session(), index);
  }

  bool selectCandidate(size_t index) {
    return rime->select_candidate(session(), index);
  }

  bool forgetCandidate(size_t index) {
    return rime->delete_candidate(session(), index);
  }

  bool changePage(bool backward) {
    return rime->change_page(session(), backward);
  }

  bool highlightCandidate(size_t index) {
    return rime->highlight_candidate(session(), index);
  }

  size_t getHighlightCandidate() {
    return rime_get_highlighted_candidate_index(session());
  }

  std::vector<CandidateItem> getCandidates(int startIndex, int limit) {
    std::vector<CandidateItem> result;
    if (startIndex < 0 || limit <= 0) {
      return result;
    }
    result.reserve(limit);

    auto current_session = rime::Service::instance().GetSession(session(false));
    if (!current_session || !current_session->context()) {
      return result;
    }
    auto& composition = current_session->context()->composition();
    if (composition.empty() || !composition.back().menu) {
      return result;
    }
    auto menu = composition.back().menu;
    size_t start = static_cast<size_t>(startIndex);
    size_t requested = start + static_cast<size_t>(limit);
    menu->Prepare(requested);
    for (size_t i = start; i < requested; ++i) {
      auto candidate = menu->GetCandidateAt(i);
      if (!candidate) {
        break;
      }
      result.emplace_back(candidate->text(), candidate->comment(),
                          isSelfCreatedCandidate(candidate));
    }
    return result;
  }

  void exit() {
    session_.reset();
    rime->finalize();
  }

  bool sync() {
    session_.reset();
    return rime->sync_user_data();
  }

 private:
  RimeApi *rime;
  std::shared_ptr<SessionHolder> session_;

  RimeSessionId session(bool requestNewSession = true) {
    if (!session_ && requestNewSession) {
      try {
        auto newSession = std::make_shared<SessionHolder>();
        session_ = newSession;
      } catch (...) {
        session_ = nullptr;
      }
    }
    if (!session_) {
      return 0;
    }
    return session_->id();
  }
};

GlobalRefSingleton *GlobalRef;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
  GlobalRef = new GlobalRefSingleton(jvm);
  declare_librime_module_dependencies();
  return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_com_osfans_trime_core_Rime_startupRime(
    JNIEnv *env, jclass clazz, jstring shared_dir, jstring user_dir,
    jstring version_name, jboolean full_check) {
  // for rime shared data dir
  setenv("RIME_SHARED_DATA_DIR", CString(env, shared_dir), 1);
  // for rime user data dir
  setenv("RIME_USER_DATA_DIR", CString(env, user_dir), 1);
  setenv("RIME_DISTRIBUTION_VERSION", CString(env, version_name), 1);

  auto notificationHandler = [](void *context_object, RimeSessionId session_id,
                                const char *message_type,
                                const char *message_value) {
    auto env = GlobalRef->AttachEnv();
    int type = 0;  // unknown
    if (strcmp(message_type, "schema") == 0) {
      type = 1;
    } else if (strcmp(message_type, "option") == 0) {
      type = 2;
    } else if (strcmp(message_type, "deploy") == 0) {
      type = 3;
    }
    auto vararg = JRef<jobjectArray>(
        env, env->NewObjectArray(1, GlobalRef->Object, nullptr));
    env->SetObjectArrayElement(vararg, 0, JString(env, message_value));
    env->CallStaticVoidMethod(GlobalRef->Rime, GlobalRef->HandleRimeMessage,
                              type, *vararg);
  };

  Rime::Instance().startup(full_check, notificationHandler);
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_exitRime(JNIEnv *env, jclass /* thiz */) {
  Rime::Instance().exit();
}

// deployment
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deployRimeSchemaFile(JNIEnv *env,
                                                     jclass /* thiz */,
                                                     jstring schema_file) {
  return Rime::Instance().deploySchema(*CString(env, schema_file));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deployRimeConfigFile(JNIEnv *env,
                                                     jclass /* thiz */,
                                                     jstring file_name,
                                                     jstring version_key) {
  return Rime::Instance().deployConfigFile(*CString(env, file_name),
                                           *CString(env, version_key));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_syncRimeUserData(JNIEnv *env,
                                                 jclass /* thiz */) {
  return Rime::Instance().sync();
}

// input
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_processRimeKey(JNIEnv *env, jclass /* thiz */,
                                               jint keycode, jint mask) {
  return Rime::Instance().processKey(keycode, mask);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_commitRimeComposition(JNIEnv *env,
                                                      jclass /* thiz */) {
  return Rime::Instance().commitComposition();
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_clearRimeComposition(JNIEnv *env,
                                                     jclass /* thiz */) {
  Rime::Instance().clearComposition();
}

// output
extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeCommit(JNIEnv *env, jclass /* thiz */) {
  rime::proto::Commit commit;
  Rime::Instance().commitProto(reinterpret_cast<uintptr_t>(&commit));
  return rimeProtoCommitToJObject(env, commit);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeContext(JNIEnv *env, jclass /* thiz */) {
  rime::proto::Context context;
  Rime::Instance().contextProto(reinterpret_cast<uintptr_t>(&context));
  return rimeProtoContextToJObject(env, context);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeStatus(JNIEnv *env, jclass /* thiz */) {
  rime::proto::Status status;
  Rime::Instance().statusProto(reinterpret_cast<uintptr_t>(&status));
  return rimeProtoStatusToJObject(env, status);
}

// runtime options
extern "C" JNIEXPORT void JNICALL Java_com_osfans_trime_core_Rime_setRimeOption(
    JNIEnv *env, jclass /* thiz */, jstring option, jboolean value) {
  Rime::Instance().setOption(*CString(env, option), value);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_getRimeOption(JNIEnv *env, jclass /* thiz */,
                                              jstring option) {
  return Rime::Instance().getOption(*CString(env, option));
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_osfans_trime_core_Rime_getRimeSchemaList(JNIEnv *env,
                                                  jclass /* thiz */) {
  return rimeSchemaListToJObjectArray(env, Rime::Instance().schemaList());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_osfans_trime_core_Rime_getCurrentRimeSchema(JNIEnv *env,
                                                     jclass /* thiz */) {
  return env->NewStringUTF(Rime::Instance().currentSchemaId().c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_selectRimeSchema(JNIEnv *env, jclass /* thiz */,
                                                 jstring schema_id) {
  return Rime::Instance().selectSchema(*CString(env, schema_id));
}

// testing
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_simulateRimeKeySequence(JNIEnv *env,
                                                        jclass /* thiz */,
                                                        jstring key_sequence) {
  return Rime::Instance().simulateKeySequence(CString(env, key_sequence));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_osfans_trime_core_Rime_getRimeRawInput(JNIEnv *env,
                                                jclass /* thiz */) {
  return env->NewStringUTF(Rime::Instance().rawInput().data());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_learnRimeRawInput(JNIEnv *env,
                                                  jclass /* thiz */,
                                                  jstring text) {
  std::string raw_text = CString(env, text);
  return Rime::Instance().learnRawInput(raw_text);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_osfans_trime_core_Rime_queryRimeRawInputCompletions(
    JNIEnv *env, jclass /* thiz */, jstring prefix, jint limit) {
  std::string raw_prefix = CString(env, prefix);
  size_t query_limit = limit > 0 ? static_cast<size_t>(limit) : 0;
  auto result = Rime::Instance().queryRawInputCompletions(
      raw_prefix, query_limit);
  auto array = env->NewObjectArray(static_cast<jsize>(result.size()),
                                   GlobalRef->String, nullptr);
  for (jsize i = 0; i < static_cast<jsize>(result.size()); ++i) {
    env->SetObjectArrayElement(array, i, JString(env, result[i]));
  }
  return array;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_osfans_trime_core_Rime_getRimeCaretPos(JNIEnv *env,
                                                jclass /* thiz */) {
  return static_cast<jint>(Rime::Instance().caretPosition());
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_setRimeCaretPos(JNIEnv *env, jclass /* thiz */,
                                                jint caret_pos) {
  Rime::Instance().setCaretPosition(caret_pos);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_selectRimeCandidateOnCurrentPage(
    JNIEnv *env, jclass /* thiz */, jint index) {
  return Rime::Instance().selectCandidateOnCurrentPage(index);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deleteRimeCandidateOnCurrentPage(
    JNIEnv *env, jclass /* thiz */, jint index) {
  return Rime::Instance().deleteCandidateOnCurrentPage(index);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_selectRimeCandidate(JNIEnv *env,
                                                    jclass /* thiz */,
                                                    jint index) {
  return Rime::Instance().selectCandidate(index);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_forgetRimeCandidate(JNIEnv *env,
                                                    jclass /* thiz */,
                                                    jint index) {
  return Rime::Instance().forgetCandidate(index);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_changeRimeCandidatePage(JNIEnv *env,
                                                        jclass clazz,
                                                        jboolean backward) {
  return Rime::Instance().changePage(backward);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_highlightRimeCandidate(JNIEnv *env,
                                                    jclass /* thiz */,
                                                    jint index) {
  return Rime::Instance().highlightCandidate(index);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_osfans_trime_core_Rime_getHighlightRimeCandidate(JNIEnv *env,
                                                       jclass /* thiz */) {
  return Rime::Instance().getHighlightCandidate();;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_osfans_trime_core_Rime_getRimeCandidates(JNIEnv *env, jclass clazz,
                                                  jint start_index,
                                                  jint limit) {
  return rimeCandidateListToJObjectArray(
      env, Rime::Instance().getCandidates(start_index, limit));
}
