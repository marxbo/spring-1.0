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
import java.lang.reflect.Parameter;
import java.util.*;

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
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

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

    private void doDispatcher(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 端口号后的路径
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        uri = uri.replaceFirst(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(uri)) {
            response.getWriter().write("404 Not Found!!!");
            return;
        }

        // 获取请求URL映射的Method
        Method m = this.handlerMapping.get(uri);
        // 请求参数Map  => http://localhost:8080/demo/query?name=marxbo&name=ma => key=name  value=["marxbo", "ma"]
        Map<String, String[]> params = request.getParameterMap();
        // 获取Method的形参列表
        Class<?>[] parameterTypes = m.getParameterTypes();
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            // 形参Class对象，不能通过它获取形参的注解
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = request;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = request;
                continue;
            } else if (parameterType == String.class) {
                // 获取Method的各个形参上的注解列表（一个方法参数上可以加多个注解）
                Annotation[][] pas = m.getParameterAnnotations();
                for (int j = 0; j < pas.length; j++) {
                    for (Annotation a : pas[j]) {
                        // 判断注解是否为@RequestParam
                        boolean flag = RequestParam.class.isInstance(a);
                        if (a instanceof RequestParam) {
                            String paramName = ((RequestParam) a).value();
                            // 若请求参数中包含该参数名称
                            if (params.containsKey(paramName)) {

                            }
                        }
                    }
                }
                RequestParam requestParam = parameterType.getAnnotation(RequestParam.class);
                if (params.containsKey(requestParam.value())) {

                }
            }
        }
        String beanName = toLowerFirstCase(m.getDeclaringClass().getSimpleName());
        m.invoke(ioc.get(beanName), paramValues);
        // m.invoke(ioc.get(beanName), request, response, params.get("name")[0]);
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

        // 2、扫描相关的类
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
     * 2、扫描相关的类
     *
     * @param scanPackage 扫描的包路径
     */
    private void doScanner(String scanPackage) {
        // classpath类路径 + 扫描包路径
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
                        // 依然使用instance
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
                handlerMapping.put(url, m);
                System.out.println("Mapped: " + url + ", " + m);
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

}
