package com.loveqrc.annotation_complier_lib;

import com.google.auto.service.AutoService;
import com.loveqrc.annotaionlib.BindView;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * 注解处理器功能，在这个类中完成
 * 可以生成代码，也可以做你想的任何事
 * 使用前一定要先注册
 */

/**
 * 使用前需要初始化三个动作
 * 1. 返回支持的java版本
 * 2. 返回需要处理的注解
 * 3. 生成文件的对象
 */
@AutoService(Processor.class)
public class AnnotationsCompiler extends AbstractProcessor {


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> types = new HashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }


    //3.需要一个用来生成文件的对象
    Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }


    /**
     * 注解处理
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获取所有标记BindView注解的元素
        Set<? extends Element> elementsAnnotatedWith
                = roundEnvironment.getElementsAnnotatedWith(BindView.class);

        //建立一个key value  key存的是标记了BindView的Activity， value是BindView注解集合
        Map<String, List<VariableElement>> map = new HashMap<>();

        //遍历添加
        for (Element element : elementsAnnotatedWith) {
            VariableElement variableElement = (VariableElement) element;
            //取到activity的名字
            String activityName = variableElement.getEnclosingElement().getSimpleName().toString();
            List<VariableElement> variableElements = map.get(activityName);
            if (variableElements == null) {
                variableElements = new ArrayList();
                map.put(activityName, variableElements);
            }
            variableElements.add(variableElement);

        }

        //有了注解之后，就可以生成对应的文件了
        if (map.size() > 0) {
            Writer writer = null;
            Iterator<String> iterator = map.keySet().iterator();
            while(iterator.hasNext()) {
                String activityName = iterator.next();
                List<VariableElement> variableElements = map.get(activityName);

                //得到包名
                TypeElement typeElement = (TypeElement)variableElements.get(0).getEnclosingElement();
                String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).toString();

                //写入文件
                try{
                    JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + activityName + "_ViewBinding");
                    writer = sourceFile.openWriter();
                    //package com.example.butterknife_framework_demo;
                    writer.write("package " + packageName + ";\n");
                    //import com.example.butterknife_framework_demo.IBinder;
                    //writer.write("import " + packageName + ".IBinder;\n");
                    writer.write("import " + "com.loveqrc.butterknife_api" + ".IBinder;\n");
                    //public class MainActivity_ViewBinding implements IBinder<com.example.butterknife_frameork_demo.MainActivity> {
                    writer.write("public class " + activityName + "_ViewBinding implements IBinder<" + packageName + "." + activityName + ">{\n");
                    //@Override
                    writer.write("@Override\n");
                    //public void bind(com.example.butterknife_framework_demo.MainActivity target) {
                    writer.write("public void bind(" + packageName + "." + activityName + " target) {\n");
                    //target.textView = (android.widget.TextView) target.findViewById(2131165359);
                    for (VariableElement variableElement: variableElements) {

                        //得到名字
                        String variableName = variableElement.getSimpleName().toString();
                        //得到ID
                        int id = variableElement.getAnnotation(BindView.class).value();
                        //得到类型
                        TypeMirror typeMirror = variableElement.asType();
                        messager.printMessage(Diagnostic.Kind.NOTE, "HHHA:1.1.1====>" + variableName + " " + id);
                        //target.textView = (android.widget.TextView) target.findViewById(2131165359);
                        writer.write("\ttarget." + variableName + "=(" + typeMirror + ")target.findViewById(" + id + ");\n");
                    }
                    writer.write("\n}\n}");

                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    if(writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }

        return false;
    }
}
