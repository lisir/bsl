package org.boilit.bsl.core.exo;

import org.boilit.acp.ACP;
import org.boilit.bsl.Context;
import org.boilit.bsl.ITemplate;
import org.boilit.bsl.core.*;
import org.boilit.bsl.exception.ExecuteException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Boilit
 * @see
 */
@SuppressWarnings("unchecked")
public final class Invoke extends AbstractOperator {
    protected AbstractExpression expression;
    private Nature[] natures;
    private List<Nature> children;

    public Invoke(final int line, final int column, final AbstractExpression expression, final ITemplate template) {
        super(line, column, template);
        this.expression = expression;
        this.children = new ArrayList<Nature>();
    }

    @Override
    public final Invoke detect() throws Exception {
        if(expression != null) {
            expression.detect();
        }
        final Nature[] natures = this.natures;
        for(int i=0, n=natures.length; i<n; i++) {
            if(natures[i] != null) {
                natures[i].detect();
            }
        }
        return this;
    }

    @Override
    public final Object execute(final Context context) throws Exception {
        Object value = expression.execute(context);
        final Nature[] natures = this.natures;
        final int n = natures.length;
        for (int i = 0; i < n; i++) {
            if(value == null) {
                return null;
            }
            if(value.getClass().isArray()) {
                value = ArrayWrapper.wrap((Object[]) value);
            }
            if (!natures[i].acting) {
                switch (natures[i].kind) {
                    case Nature.FIELD : {
                        natures[i].proxy = ACP.proxyField(value.getClass(), natures[i].label);
                        break;
                    }
                    case Nature.METHOD : {
                        Object[] parameters = (Object[]) natures[i].execute(context);
                        natures[i].proxy = ACP.proxyMethod(value.getClass(), natures[i].label, parameters);
                        break;
                    }
                }
                natures[i].acting = true;
            }
            if (natures[i].proxy == null) {
                throw new ExecuteException(natures[i], "Can't proxy[" + natures[i].label + "]!");
            }
            switch (natures[i].kind) {
                case Nature.FIELD : {
                    value = natures[i].proxy.invoke(value, null);
                    break;
                }
                case Nature.METHOD : {
                    value = natures[i].proxy.invoke(value, (Object[])natures[i].execute(context));
                    break;
                }
            }
        }
        if (value == null) {
            return null;
        }
        if(value.getClass().isArray()) {
            value = ArrayWrapper.wrap((Object[]) value);
        }
        return value;
    }

    @Override
    public final AbstractExpression optimize() throws Exception {
        if ((expression = expression.optimize()) == null) {
            return null;
        }
        natures = new Nature[children.size()];
        children.toArray(natures);
        children.clear();
        children = null;
        return this;
    }

    public final Invoke add(Nature expression) throws Exception {
        if ((expression = expression.optimize()) == null) {
            return this;
        }
        this.children.add(expression);
        return this;
    }
}
