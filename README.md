# APT
`APT`全称是`Annotation Processing Tool`即注解处理器，在`ButterKnife`中，我们使用`ButterKnife`提供的`@BindView`注解绑定一个控件后，`ButterKnife`会通过`APT`注解处理器，翻译解析这个注解，最后生成一个文件，达到无需手动`findView`的目的。下面来尝试一下如何是实现`ButterKnife`的效果。

![ButterKnife使用demo](https://upload-images.jianshu.io/upload_images/2018603-57f692dacfda5d5b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



#  一 提供注解
新建一个`Java Library`用于提供注解

![注解lib](https://upload-images.jianshu.io/upload_images/2018603-3f7aa1ed8488a817.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> BindView
```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface BindView {
    //可以得到@BindView(R.id.btn)中间括号里面的内容
    int value();
}
```
# 二  处理注解
有了注解之后，就可以通过`APT`技术，获取注解的内容，新建一个`Java Library`用于处理注解。

![注解处理lib](https://upload-images.jianshu.io/upload_images/2018603-efabbe8b43f7af06.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> build.gradle

添加实现注解处理器依赖

```
     // 依赖注解提供库
     api project(path: ':annotaionlib')
    //添加APT依赖
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
    compileOnly 'com.google.auto.service:auto-service:1.0-rc6'
```

> AnnotationsCompiler.class

```
@AutoService(Processor.class)
public class AnnotationsCompiler extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return false;
    }
}
```
实现注解处理的准备工作有如下
1. 添加`@AutoService(Processor.class)` 和继承`AbstractProcessor`
2. 返回支持的java版本
3. 返回需要处理的注解
4. 生成文件的对象

```
@AutoService(Processor.class)
public class AnnotationsCompiler extends AbstractProcessor {
    
    // 返回支持的java版本,这里是最新版本
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    
     //返回需要处理的注解,这里只处理BindView注解
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> types = new HashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }


    
    Filer filer;
    private Messager messager;
     //初始化工程，生成文件对象，后面生成文件的时候会用到
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return false;
    }
}
```

准备好上面的步骤后，可以实现`process`方法了。


在实现`process`方法之前，回想一下如果我们不使用`APT`工具，那么该怎么实现该功能？`APT`的作用就是按照制定的规则实现文件，要制定规则，必须清楚要实现的文件应该是怎么样？

##  `APT`处理后，需要实现的文件
```
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn)
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        new MainActivity_ViewBinding().bind(this);
        btn.setText("Rc在努力");
    }
}

class MainActivity_ViewBinding {
    public void bind(MainActivity target) {
        target.btn= (Button) target.findViewById(R.id.btn);
    }
}
```

上面是不使用`APT`，而是用代码实现`APT`的功能，现在只需把上面的功能改为`APT`自动实现就行了，有了思路就好解决。
```
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
```

## 实例化生成后的文件

新建一个`Android Libray`用于实例化生成的文件。
![WX20200727-160243@2x.png](https://upload-images.jianshu.io/upload_images/2018603-2c4d9f8ed3a96379.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> IBinder

```
public interface IBinder<T> {
    void bind(T target);
}
```

> ButterKnife
```
public class ButterKnife {
    public static void bind(Activity activity) {
        String name = activity.getClass().getName() + "_ViewBinding";
        try {
            Class<?> aClass = Class.forName(name);
            IBinder iBinder = (IBinder)aClass.newInstance();
            iBinder.bind(activity);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
`IBinder`用于给所有`APT`生成的类实现，这样做的好处是类似于面向接口编程，`ButterKnife`的`bind`用于的绑定生成文件和`Activity`之间的关系。这里用到了一次反射。

# [源码](https://github.com/LoveqLRC/ButterknifeDemo)

