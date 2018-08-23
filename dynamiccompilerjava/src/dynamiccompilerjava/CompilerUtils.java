package dynamiccompilerjava;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import constant.CMDConstant;
import constant.CodingConstant;
import constant.CommonSymbolicConstant;
import constant.ConfigConstant;
import constant.FileExtensionConstant;
import constant.OtherConstant;
import constant.SpiderConstant;

public class CompilerUtils {
	// 编译,java文件目录，jar包路径，搜索资源路径，编译存放路径，指定编译器路径
	public static boolean compilerJAVACodeFile(String filePath, String jarPath, String sourceDir, String targetDir,
			String javaCompilerPath) {
		// 注意路径不同
		// 调用封装好的编译方法，需要提前准备好参数
		// 参数分别是：// 编码格式，jar包，需要编译的目录，关联查找目录，编译后存放目录，错误信息
		// 编码格式
		String encoding = CodingConstant.CODING_UTF_8;
		// jar包路径拼接字符串
		String jars = CommonSymbolicConstant.EMPTY_STRING;
		// 开始挨个获取
		// 获取jar包拼接字符串
		try {
			jars = getJarFiles(jarPath);
		} catch (Exception e) {
			e.printStackTrace();
			jars = CommonSymbolicConstant.EMPTY_STRING;
		}
		// 错误信息
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		// 开始编译
		try {
			if (!compiler(encoding, jars, filePath, filePath, targetDir, diagnostics, javaCompilerPath)) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	// 查找jar
	public static String getJarFiles(String jarPath) throws Exception {
		String jars = CommonSymbolicConstant.EMPTY_STRING;
		File sourceFile = new File(jarPath);
		if (sourceFile.exists()) {
			if (sourceFile.isDirectory()) {
				// 得到该目录下以.jar结尾的文件或者目录
				File[] childrenFiles = sourceFile.listFiles();
				jars = filterJarFile(childrenFiles);
			}
		}
		return jars;
	}

	// 过滤文件
	private static String filterJarFile(File[] childrenFiles) {
		String jars = CommonSymbolicConstant.EMPTY_STRING;
		for (File pathname : childrenFiles) {
			if (!pathname.isDirectory()) {
				String name = pathname.getName();
				if (name.endsWith(FileExtensionConstant.FILE_EXTENSION_POINT_CODEFILE_JAR) ? true : false) {
					jars = jars + pathname.getPath() + CommonSymbolicConstant.SEMICOLON;
				}
			}
		}
		return jars;
	}

	// 编码格式，jar包，需要编译的目录，关联查找目录，编译后存放目录，错误信息
	public static boolean compiler(String encoding, String jars, String filePath, String sourceDir, String targetDir,
			DiagnosticCollector<JavaFileObject> diagnostics, String javaCompilerPath) throws Exception {
		// 获取编译器实例
		// JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		// 此处无法加载如果没有安装jdk,另为了安全起见需要使用单独的jre
		// 解决方法一，通过手动加载tools.jar获取相关类方法获取编译器再编译
		// 解决方法二，通过Runtime方法执行cmd命令设置Javahome
		JavaCompiler compiler = null;
		try {
			// 获取指定的编译器（需要tools.jar）
			File file = new File(javaCompilerPath == null ? "" : javaCompilerPath);
			if (!file.exists()) {
				// 获取系统编译器需要把jdk中的tool.jar复制到jre中，因为自带的jre没有
				// compiler = ToolProvider.getSystemJavaCompiler();
				// 获取系统变量
				String javahome = System.getenv(ConfigConstant.CONFIG_ENV_JAVA_HOME);
				file = new File(javahome + File.separator + ConfigConstant.CONFIG_DEFAULT_TOOLS_PATH);
				if (!file.exists()) {
					return false;
				}
			}
			compiler = getJavaCompilerByLocation(file);
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
		// 获取标准文件管理器实例
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		try {
			// 得到filePath目录下的所有java源文件
			File sourceFile = new File(filePath);
			List<File> sourceFileList = new ArrayList<File>();
			// 根据文件判断是否是java文件,并存放List
			getSourceFiles(sourceFile, sourceFileList);
			// 没有java文件，直接返回
			if (sourceFileList.size() == 0) {
				return false;
			}
			// 获取要编译的编译单元
			Iterable<? extends JavaFileObject> compilationUnits = fileManager
					.getJavaFileObjectsFromFiles(sourceFileList);
			// 编译选项，在编译java文件时，编译程序会自动的去寻找java文件引用的其他的java源文件或者class。
			// -sourcepath选项就是定义java源文件的查找目录， -classpath选项就是定义class文件的查找目录。
			Iterable<String> options = Arrays.asList(CMDConstant.CMD_COMMAND_JAVACOMPILE_1, encoding,
					CMDConstant.CMD_COMMAND_JAVACOMPILE_2, jars, CMDConstant.CMD_COMMAND_JAVACOMPILE_3, targetDir,
					CMDConstant.CMD_COMMAND_JAVACOMPILE_4, sourceDir);
			CompilationTask compilationTask = compiler.getTask(null, fileManager, diagnostics, options, null,
					compilationUnits);
			// 运行编译任务
			return compilationTask.call();
		} finally {
			fileManager.close();
		}
	}

	// 获取非系统编译器
	public static JavaCompiler getJavaCompilerByLocation(File f1) throws Exception {
		// tool.jar我放在了本项目根目录下，可自行更改
		String p = f1.getAbsolutePath();
		URL[] urls = new URL[] { new URL(OtherConstant.OTHER_URL_1 + p) };
		URLClassLoader myClassLoader1 = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		Class<?> myClass1 = myClassLoader1.loadClass(OtherConstant.OTHER_JAVACTOOL_CLASS);
		JavaCompiler compiler = myClass1.asSubclass(JavaCompiler.class).asSubclass(JavaCompiler.class).newInstance();
		return compiler;
	}

	// 判断不为空
	public static boolean isnull(String str) {
		if (null == str) {
			return false;
		} else if (CommonSymbolicConstant.EMPTY_STRING.equals(str)) {
			return false;
		} else if (str.equals(CommonSymbolicConstant.NULL)) {
			return false;
		} else {
			return true;
		}
	}

	// 递归查找java文件
	private static void getSourceFiles(File sourceFile, List<File> sourceFileList) throws Exception {
		if (sourceFile.exists() && sourceFileList != null) {
			if (sourceFile.isDirectory()) {
				// 得到该目录下以.java结尾的文件或者目录
				File[] childrenFiles = sourceFile.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						if (pathname.isDirectory()) {
							return true;
						} else {
							String name = pathname.getName();
							if (name.endsWith(FileExtensionConstant.FILE_EXTENSION_POINT_CODEFILE_JAVA) ? true
									: false) {
								return true;
							}
							return false;
						}
					}
				});
				// 递归调用
				for (File childFile : childrenFiles) {
					getSourceFiles(childFile, sourceFileList);
				}
			} else {// 若file对象为文件
				sourceFileList.add(sourceFile);
			}
		}
	}

}
