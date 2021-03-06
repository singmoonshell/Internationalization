package com.hyc;

import com.hyc.bean.Language;
import com.hyc.utils.ConvertHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SelectFromListDialog;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import org.apache.http.util.TextUtils;

import javax.swing.*;


public class Internationalization extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiFile file = e.getData(DataKeys.PSI_FILE);
        if (!checkFile(file)) {
            Messages.showErrorDialog("错误","请选择strings.xml");
            return;
        }
        XmlFile xmlFile = (XmlFile) file;
        ConvertHelper convertHelper = new ConvertHelper(xmlFile);
        String targetCode = getTargetLanguageCode(e);
        if (!TextUtils.isEmpty(targetCode)){
            XmlDocument xmlDocument = convertHelper.convert(targetCode);
            generateTargetFile(xmlFile, targetCode, xmlDocument);
        }
    }

    private void generateTargetFile(XmlFile xmlFile, String targetCode, XmlDocument xmlDocument) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            PsiDirectory directory = null;
            for (PsiDirectory subdirectory : xmlFile.getParent().getParent().getSubdirectories()) {
                if (subdirectory.getName().equals("values-" + targetCode)) {
                    directory = subdirectory;
                    break;
                }
            }
            if (directory == null) {
                directory = xmlFile.getParent().getParent().createSubdirectory("values-" + targetCode);
            }
            XmlFile targetFile = null;
            for (PsiFile directoryFile : directory.getFiles()) {
                if (directoryFile.getName().equals("strings.xml")) {
                    targetFile = (XmlFile) directoryFile;
                    break;
                }
            }
            if (targetFile != null) {
                targetFile.delete();
            }
            targetFile = (XmlFile) directory.createFile("strings.xml");

            XmlFile finalTargetFile = targetFile;
            WriteCommandAction.runWriteCommandAction(xmlFile.getProject(), () -> {
                        finalTargetFile.getDocument().add(xmlDocument);
                    }
            );
        });
    }

    private boolean checkFile(PsiFile file) {
        return file instanceof XmlFile && file.getName().equals("strings.xml");
    }

    private String getTargetLanguageCode(AnActionEvent e) {
        SelectFromListDialog dialog = new SelectFromListDialog(e.getProject(), Language.getAllLanguage(), o -> o.toString(), "选择目标语言", ListSelectionModel.SINGLE_SELECTION);
        dialog.show();
        Object[] selects =  dialog.getSelection();
        if (selects == null || selects.length == 0){
            return null;
        }
        String select = selects[0].toString();
        return select.split(" ")[1];
    }
}
