package com.zw.beanconverter;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.zw.beanconverter.utils.StringUtil;

/**
 * Created by zw on 2019/4/3.
 */
public class BeanConverterAction extends AnAction {

    //class name
    private static String CLASS_NAME;

    private static PsiElementFactory psiElementFactory;

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        generateBeanConverter(this.getPsiMethodFromContext(anActionEvent));
    }

    private PsiClass getPsiMethodFromContext(AnActionEvent e) {
        PsiElement elementAt = this.getPsiElement(e);
        return elementAt == null ? null : PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private PsiElement getPsiElement(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile != null && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            return psiFile.findElementAt(offset);
        } else {
            e.getPresentation().setEnabled(false);
            return null;
        }
    }

    private void generateBeanConverter(final PsiClass psiMethod) {
        CLASS_NAME = psiMethod.getName();
        psiElementFactory = JavaPsiFacade.getElementFactory(psiMethod.getProject());
        (new WriteCommandAction.Simple(psiMethod.getProject(), new PsiFile[]{psiMethod.getContainingFile()}) {
            protected void run() throws Throwable {
                createConverterMethod(psiMethod);
            }
        }).execute();
    }

    private void createConverterMethod(PsiClass psiClass) {
        //create bean name
        String beanName = StringUtil.firstCharToLowerCase(CLASS_NAME);

        //create private method and return type is class name
        String methodName = beanName + "Converter";
        PsiMethod converterMethod = psiElementFactory.createMethod(methodName,
                psiElementFactory.createTypeFromText(CLASS_NAME, null));
        converterMethod.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);

        //create parameter of method
        String parameterName = "obj";
        PsiParameter setterParameter = psiElementFactory.createParameter(parameterName,
                psiElementFactory.createTypeFromText("Object", null));
        converterMethod.getParameterList().add(setterParameter);

        //check fields
        PsiField[] fields1 = psiClass.getFields();
        if (0 == fields1.length) {
            return;
        }
        PsiCodeBlock methodBody = converterMethod.getBody();
        if (methodBody == null) {
            return;
        }

        //new bean  :   Bean bean = new Bean();
        PsiStatement newBeanStatement = psiElementFactory.createStatementFromText(String.format(
                "%s %s = new %s();", CLASS_NAME, beanName, CLASS_NAME), converterMethod);
        methodBody.add(newBeanStatement);

        //set field value statement :bean.setField(obj.getField());
        for (PsiField field : fields1) {
            if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                String fieldName = StringUtil.capitalize(field.getName());
                PsiStatement setStatement = psiElementFactory.createStatementFromText(String.format(
                        "%s.set%s(%s.get%s());", beanName, fieldName, parameterName, fieldName), converterMethod);
                methodBody.add(setStatement);
            }
        }

        //return statement: return bean;
        PsiStatement setStatement = psiElementFactory.createStatementFromText(String.format(
                "return %s;", beanName), converterMethod);
        methodBody.add(setStatement);

        //add method
        psiClass.add(converterMethod);
    }
}
