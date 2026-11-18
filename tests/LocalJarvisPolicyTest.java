import fr.friday.assistant.LocalJarvisPolicy;
import fr.friday.assistant.LocalJarvisArithmetic;

public final class LocalJarvisPolicyTest {
    static void ok(boolean v,String n){if(!v)throw new AssertionError(n);}
    public static void main(String[] args){
        ok(LocalJarvisPolicy.explicit("Friday, Jarvis, explique-moi"),"déclencheur vocal");
        ok(LocalJarvisPolicy.explicit("Friday, demande à Jarvis de résumer"),"délégation");
        ok(!LocalJarvisPolicy.explicit("Friday, actualités du jour"),"Mistral reste par défaut");
        ok(!LocalJarvisPolicy.shouldRoute("Jarvis, réponds",false,true),"activation requise");
        ok(LocalJarvisPolicy.shouldRoute("Jarvis, réponds",true,false),"réponse locale");
        ok(LocalJarvisPolicy.shouldRoute("actualités",true,true),"option générale");
        ok(LocalJarvisPolicy.cleanQuestion("Friday, Jarvis, explique les volcans")
                 .equals("explique les volcans"),"préfixe retiré");
        ok(LocalJarvisPolicy.cleanQuestion("Friday, demande à Jarvis de calculer")
                 .equals("de calculer"),"délégation nettoyée");
        ok(LocalJarvisArithmetic.tryAnswer("calcule 2+3*4").contains("14"),"priorités");
        ok(LocalJarvisArithmetic.tryAnswer("calcule (2+3)*4").contains("20"),"parenthèses");
        ok(LocalJarvisArithmetic.tryAnswer("calcule 9/0").contains("invalide"),"division zéro");
        ok(LocalJarvisArithmetic.tryAnswer("bonjour") == null,"pas de calcul inventé");
        ok(LocalJarvisArithmetic.tryAnswer("calcule 10-2").contains("8"),"soustraction");
        System.out.println("LocalJarvisPolicyTest OK");
    }
}
