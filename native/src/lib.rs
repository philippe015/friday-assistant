//! Détection du mot-clé dans les fragments complets de Vosk.
//! Le microphone et le réveil de l'appareil restent gérés par Android Java.
use jni::{JNIEnv, objects::{JClass, JString}, sys::{jboolean, JNI_TRUE, JNI_FALSE}};

fn wake_word(text: &str) -> bool {
    text.split(|c: char| !c.is_alphanumeric())
        .any(|w| matches!(w, "jarvis" | "jarvise" | "jarvice" | "jervis" | "jervi"))
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_fr_friday_assistant_RustWake_hasWakeWord(
    mut env: JNIEnv, _class: JClass, transcription: JString,
) -> jboolean {
    let phrase: String = match env.get_string(&transcription) {
        Ok(value) => value.into(),
        Err(_) => return JNI_FALSE,
    };
    if wake_word(&phrase.to_lowercase()) { JNI_TRUE } else { JNI_FALSE }
}

#[cfg(test)]
mod tests {
    use super::wake_word;
    #[test]
    fn activation() {
        assert!(wake_word("jarvis quelle heure est il"));
        assert!(wake_word("bonjour jarvis"));
        assert!(!wake_word("un vendredi normal"));
        assert!(!wake_word("friday quel temps fait il"));
        assert!(!wake_word("jarvisx"));
    }
}
