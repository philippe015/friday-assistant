package fr.friday.assistant;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

/** Appelle le module Python inclus dans l'APK. Aucun serveur local ni Termux requis. */
public final class PythonBrain {
    private PythonBrain() {}
    public static String localAnswer(String question) {
        try {
            PyObject result = Python.getInstance().getModule("friday_logic")
                    .callAttr("local_answer", question, System.currentTimeMillis());
            if (result == null) return null;
            String value = result.toString();
            return value.isEmpty() ? null : value;
        } catch (Exception error) {
            return NativeAI.localAnswer(question);
        }
    }
    public static String prepareQuestion(String question) {
        try {
            return Python.getInstance().getModule("friday_logic")
                    .callAttr("prepare_question", question).toString();
        } catch (Exception error) {
            return question;
        }
    }
}
