package jdolly;

import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4compiler.ast.CommandScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JDollyEasy extends JDolly {

    private Scope scope;

    public JDollyEasy(Scope scope, String alloyTheory) {
        this.scope = scope;
        this.alloyTheory = alloyTheory;
    }

    protected ConstList<CommandScope> createScopeList() throws ErrorSyntax {
        Map<String, Integer> sigs = this.scope.getSigs();

        List<CommandScope> result = new ArrayList<>();

        for (String key : sigs.keySet()) {
            result.add(new CommandScope(this.createSignatureBy(key), false, sigs.get(key)));
        }

        return ConstList.make(result);
    }

    @Override
    protected void initializeAlloyAnalyzer() {
        try {
            defineCurrentAns(createA4Reporter());
        } catch (Err e) {
            e.printStackTrace();
        }
    }

}
