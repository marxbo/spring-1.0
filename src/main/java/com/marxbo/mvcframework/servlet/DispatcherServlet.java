package com.marxbo.mvcframework.servlet;

import com.marxbo.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 核心控制器
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/3/18 23:38
 */
public class DispatcherServlet extends HttpServlet {

    /** application.properties配置文件 */
    private Properties contextConfig = new Properties();
    /** 扫描包下的所有类名 */
    private List<String> classNames = new ArrayList<String>();
    /** IOC容器 */
    private Map<String, Object> ioc = new HashMap<String, Object>();
    /** 保存URL和Method的对应关系 */
    //private Map<String, Method> handlerMapping = new HashMap<String, Method>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 6、调用、运行阶段
        try {
            doDispatcher(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求委派
     *
     * @param request 请求
     * @param response 响应
     * @throws Exception
     */
    private void doDispatcher(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Handler handler = this.getHandler(request);
        //if (!this.handlerMapping.containsKey(uri)) {
        if (handler == null) {
            response.getWriter().write("404 Not Found!!!");
            return;
        }
        // 获取请求URL映射的Method
        //Method m = this.handlerMapping.get(uri);

        // 请求参数Map  => http://localhost:8080/demo/query?name=marxbo&name=ma => key=name  value=["marxbo", "ma"]
        Map<String, String[]> parameterMap = request.getParameterMap();
        // 获取Method的形参列表
        // Class<?>[] parameterTypes = m.getParameterTypes();
        Class<?>[] parameterTypes = handler.method.getParameterTypes();
        Object[] paramValues = new Object[parameterTypes.length];

        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }

            System.out.println(Arrays.toString(param.getValue()));
            String value = Arrays.toString(parameterMap.get(param.getKey()))
                    .replaceAll("\\[|\\]", "")
                    .replaceAll(",\\s", ",");
            // 根据参数名称获取形参的索引
            Integer index = handler.paramIndexMapping.get(param.getKey());
            // 转化类型并设值到实参列表
            paramValues[index] = this.convert(parameterTypes[index], value);
        }

        if (handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            Integer reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
        }
        if (handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            Integer respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
        }


        /*for (int i = 0; i < parameterTypes.length; i++) {
            // 形参Class对象，不能通过它获取形参的注解
            Class<?> parameterType = parameterTypes[i];
            // 判断类型对象是否相同直接用==
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = request;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = response;
                continue;
            } else if (parameterType == String.class) {
                // 获取Method的各个形参上的注解列表(注：一个方法参数上可以加多个注解)
                // 错误示范：RequestParam requestParam = parameterType.getAnnotation(RequestParam.class);
                Annotation[][] pas = m.getParameterAnnotations();
                for (int j = 0; j < pas.length; j++) {
                    for (Annotation a : pas[j]) {
                        // 判断注解是否为@RequestParam的2种方法
                        // boolean isRequestParam = a instanceof RequestParam;
                        boolean isRequestParam = RequestParam.class.isInstance(a);
                        if (isRequestParam) {
                            String paramName = ((RequestParam) a).value();
                            // 若参数注解的value不为空
                            if (!"".equals(paramName.trim())) {
                                System.out.println(Arrays.toString(parameterMap.get(paramName)));
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]", "")
                                        .replaceAll(",\\s", ",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }*/
        // String beanName = toLowerFirstCase(m.getDeclaringClass().getSimpleName());
        // m.invoke(ioc.get(beanName), paramValues);
        Object returnValue = handler.method.invoke(handler.controller, paramValues);
        if (returnValue == null || returnValue instanceof Void) {
            return;
        }
        response.getWriter().write(returnValue.toString());
    }

    /**
     * 初始化阶段
     *
     * @param config Servlet配置
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2、扫描包下的所有类
        doScanner((String) contextConfig.get("scanPackage"));
        // 3、初始化扫描到的类，加入到IOC容器中
        doInstance();
        // 4、完成DI依赖注入
        doAutowired();
        // 5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("Spring Framework is inited......");
    }

    /**
     * 1、加载配置文件
     *
     * @param contextConfigLocation Servlet初始化配置-配置文件路径
     */
    private void doLoadConfig(String contextConfigLocation) {
        // 获取类路径的配置文件（getClassLoader()本身就是相对类路径，不需要加"/"；去掉getClassLoader()则需要加）
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            // 加载web.xml中的Servlet初始化配置contextConfigLocation指定的文件
            contextConfig.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 2、扫描包下的所有类
     *
     * @param scanPackage 扫描的包路径
     */
    private void doScanner(String scanPackage) {
        // classpath类路径 + 扫描包路径(包路径转为文件路径)
        String path = this.getClass()
                .getResource("/" + scanPackage.replaceAll("\\.", "/"))
                .getPath();
        File classpath = new File(path);
        for (File file : classpath.listFiles()) {
            if (file.isDirectory()) {
                // 递归扫描子包
                doScanner(scanPackage + "." + file.getName());
            } else {
                // 非.class文件
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 3、初始化扫描到的类，加入到IOC容器中
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            /**
             * 什么类才需要初始化？ 答：加了注解的类才初始化。
             * isAnnotationPresent()判断该类上是否加了指定注解
             */
            try {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(Controller.class)) {
                    Object instance = clazz.newInstance();
                    // 默认类名首字母小写  注：clazz.getSimpleName()获取类名；clazz.getName()获取类的全限定名
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    // key=>类名；value=>类名
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    // 获取@Service注解的value属性
                    Service service = clazz.getAnnotation(Service.class);
                    // 1、自定义的beanName
                    String beanName = service.value();
                    // 2、默认类名首字母小写
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    // 3、根据类型自动赋值
                    for (Class<?> i : clazz.getInterfaces()) {
                        // 一个接口不允许有多个实现类
                        if (ioc.containsKey(toLowerFirstCase(i.getName()))) {
                            throw new Exception("The \"" + i.getName() + "\" is exists!");
                        }
                        // key：接口名首字母小写；value：依然使用instance
                        ioc.put(toLowerFirstCase(i.getSimpleName()), instance);
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 4、完成DI依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // Declared：所有的的字段，包括 private/protected/default
            // 正常来说，普通的OOP编程只能拿到 public 的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field f : fields) {
                // 判断成员属性是否需要依赖注入
                if (!f.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                Autowired autowired = f.getAnnotation(Autowired.class);
                // 自定义的beanName
                String beanName = autowired.value().trim();
                // 类名首字母小写
                if ("".equals(beanName)) {
                    // field.getType() => com.marxbo.demo.service.DemoService
                    // field.getDeclaringClass() => com.marxbo.demo.controller.DemoController
                    // field.getClass() => java.lang.reflect.Field
                    beanName = toLowerFirstCase(f.getType().getSimpleName());
                }
                try {
                    // 私有成员取消访问检查
                    f.setAccessible(true);
                    // 反射动态给成员属性赋值
                    f.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 5、初始化URL和Method的一对一对应关系
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            // 忽略没有加@Controller注解的类
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            // 获取Controller的URL配置
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }
            // 获取Method的URL配置
            for (Method m : clazz.getMethods()) {
                // 忽略没有加@RequestMapping注解的Method
                if (!m.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                // 映射URL
                RequestMapping requestMapping = m.getAnnotation(RequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value())
                        .replaceAll("/+", "/");
                //handlerMapping.put(url, m);
                Pattern pattern = Pattern.compile(url);
                handlerMapping.add(new Handler(pattern, entry.getValue(), m));
                System.out.println("Mapped: " + url + " ==> " + m);
            }
        }
    }

    /**
     * Java类名转首字母小写的类名
     *
     * @param simpleName Java类名
     * @return 首字母小写的类名
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 记录Controller种RequestMapping和Method的关系
     */
    private class Handler {
        /** URL映射Pattern */
        protected Pattern pattern;
        /** 保存方法对应的实例 */
        protected Object controller;
        /** 保存映射的方法 */
        protected Method method;
        /** 参数顺序 */
        protected Map<String, Integer> paramIndexMapping;

        protected Handler(Pattern pattern, Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<>();
            this.putParamIndexMapping(method);
        }

        /**
         * 解析方法参数
         *
         * @param method 方法
         */
        private void putParamIndexMapping(Method method) {
            // 提取方法中加了注解的参数
            Annotation[][] pas = method.getParameterAnnotations();
            for (int i = 0; i < pas.length; i++) {
                // 遍历一个参数的多个注解
                for (Annotation a : pas[i]) {
                    boolean isRequestParam = RequestParam.class.isInstance(a);
                    if (isRequestParam) {
                        String paramName = ((RequestParam) a).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName,i);
                        }
                    }
                }
            }
            // 提取方法中的request和response参数
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }

    /**
     * 根据请求获取处理器
     *
     * @param request 请求
     * @return 处理器
     */
    private Handler getHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        // 端口号后的路径
        String uri = request.getRequestURI();
        // 应用上下文路径
        String contextPath = request.getContextPath();
        // 去掉上下文路径，替换//为/
        uri = uri.replaceFirst(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlerMapping) {
            if (handler.pattern.matcher(uri).find()) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 参数类型转化
     *
     * @param type 类型
     * @param value 参数
     * @return 对应类型参数
     */
    private Object convert(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        // 多种类型if判断省略...
        return value;
    }

}
