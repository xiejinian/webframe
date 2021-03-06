
package org.webframe.support.driver.resource.jar;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.webframe.support.util.ClassUtils;
import org.webframe.support.util.ResourceUtils;
import org.webframe.support.util.StringUtils;

/**
 * JarResource资源类，用于匹配搜索jar包中的资源文件
 * 
 * @author <a href="mailto:guoqing.huang@foxmail.com">黄国庆 </a>
 * @version $Id: codetemplates.xml,v 1.1 2009/09/07 08:48:12 Exp $ Create: 2011-4-6 下午04:40:09
 */
public class JarResourcePatternResolver
			extends
				PathMatchingResourcePatternResolver {

	protected static int															createdSize			= 0;

	private Class<?>																jarClass				= null;

	private JarURLConnection													jarCon				= null;

	private JarResourceLoader													jarResourceLoader	= null;

	private static boolean														sorted				= false;

	private static final Map<String, JarResourcePatternResolver>	cachedMap			= new HashMap<String, JarResourcePatternResolver>(16);

	private static final Map<String, JarResourcePatternResolver>	sortedCachedMap	= new LinkedHashMap<String, JarResourcePatternResolver>(16, 0.75f, true);

	public static JarResourcePatternResolver getInstance(Class<?> clazz)
				throws IOException {
		JarResourcePatternResolver resolver = getJarResourcePatternResolver(clazz);
		if (resolver != null) {
			return resolver;
		}
		return new JarResourcePatternResolver(clazz);
	}

	public static JarResourcePatternResolver getInstance(JarURLConnection jarURLConnection)
				throws IOException {
		List<JarResourcePatternResolver> resolvers = getJarResourcePatternResolver(jarURLConnection.getEntryName());
		String path = jarURLConnection.getJarFile().getName();
		for (JarResourcePatternResolver resolver : resolvers) {
			String name = resolver.getJarResourceLoader().getJarShortName();
			if (path.endsWith(name)) {
				return resolver;
			}
		}
		return new JarResourcePatternResolver(jarURLConnection);
	}

	/**
	 * 获取JarResourcePatternResolver实例
	 * 
	 * @param clazz Class
	 * @return 实例或null
	 * @author 黄国庆 2012-5-9 下午1:28:31
	 */
	public static JarResourcePatternResolver getJarResourcePatternResolver(Class<?> clazz) {
		String key = getKey(clazz);
		boolean exist = cachedMap.containsKey(key);
		if (exist) {
			return cachedMap.get(key);
		}
		for (JarResourcePatternResolver resolver : cachedMap.values()) {
			if (resolver.hasResource(clazz)) {
				return resolver;
			}
		}
		return null;
	}

	/**
	 * 获取JarResourcePatternResolver实例集合
	 * 
	 * @param pathOrKey 路径或key
	 * @return 实例或null
	 * @author 黄国庆 2012-5-9 下午1:27:46
	 */
	public static List<JarResourcePatternResolver> getJarResourcePatternResolver(String pathOrKey) {
		List<JarResourcePatternResolver> resolvers = new ArrayList<JarResourcePatternResolver>(4);
		boolean exist = cachedMap.containsKey(pathOrKey);
		if (exist) {
			resolvers.add(cachedMap.get(pathOrKey));
			return resolvers;
		}
		for (JarResourcePatternResolver resolver : sortedCachedMap.values()) {
			if (resolver.hasResource(pathOrKey)) {
				resolvers.add(resolver);
			}
		}
		return resolvers;
	}

	/**
	 * @return
	 * @author 黄国庆 2012-5-10 下午12:39:20
	 */
	public static Collection<JarResourcePatternResolver> getAllSortedResolver() {
		if (sorted) {
			return sortedCachedMap.values();
		}
		throw new ArrayStoreException("JarResourcePatternResolver未排序，请先调用JarResourcePatternResolver.sortResolver(List<String> sortedKeys)进行排序!");
	}

	/**
	 * 根据给定已排序的key集合，对sortedCachedMap进行排序
	 * 
	 * @param sortedKeys
	 * @author 黄国庆 2012-5-10 上午10:20:42
	 */
	public static void sortResolver(List<String> sortedKeys) {
		if (sortedKeys == null) {
			return;
		}
		sorted = true;
		Set<String> noSortedKeys = new HashSet<String>(sortedCachedMap.keySet());
		for (String sortedKey : sortedKeys) {
			sortedCachedMap.get(sortedKey);
			if (noSortedKeys.contains(sortedKey)) {
				noSortedKeys.remove(sortedKey);
			}
		}
		for (String noSortedKey : noSortedKeys) {
			sortedCachedMap.get(noSortedKey);
		}
	}

	protected JarResourcePatternResolver(Class<?> jarClass) throws IOException {
		this(new JarResourceLoader(jarClass));
		this.jarClass = jarClass;
		cached();
	}

	protected JarResourcePatternResolver(JarURLConnection jarURLConnection)
				throws IOException {
		this(new JarResourceLoader(jarURLConnection));
		this.jarCon = jarURLConnection;
		cached();
	}

	private JarResourcePatternResolver(JarResourceLoader jarResourceLoader) {
		super(jarResourceLoader);
		this.jarResourceLoader = jarResourceLoader;
	}

	/**
	 * 缓存JarResourcePatternResolver
	 * 
	 * @author 黄国庆 2012-5-9 下午1:38:25
	 * @throws IOException
	 */
	protected synchronized void cached() throws IOException {
		String key = null;
		if (jarResourceLoader.hasJarResource()) {
			String shortName = jarResourceLoader.getJarShortName();
			if (shortName.lastIndexOf("-") != -1) {
				key = StringUtils.getArtifactId(shortName);
			} else {
				key = shortName;
			}
		} else {
			key = getKey(jarClass);
		}
		createdSize++;
		if (!cachedMap.containsKey(key)) {
			sortedCachedMap.put(key, this);
			cachedMap.put(key, this);
		}
	}

	/**
	 * 获取key，如果class在jar包中返回null；否则返回class的根目录
	 * 
	 * @param clazz class
	 * @return null or key
	 * @author 黄国庆 2012-5-9 下午2:06:18
	 */
	private static String getKey(Class<?> clazz) {
		Resource resource = ClassUtils.getClassesRootResource(clazz);
		if (resource == null) {
			return null;
		}
		try {
			return resource.getFile().getAbsolutePath();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (!locationPattern.startsWith("/")) {
			locationPattern = "/" + locationPattern;
		}
		if (jarCon != null || ClassUtils.isInJar(this.jarClass)) {
			return findPathMatchingJarResources(locationPattern);
		} else {
			Resource classRoot = ClassUtils.getClassesRootResource(this.jarClass);
			String rootPath = ResourceUtils.getAbsolutePath(classRoot);
			String directory = determineRootDir(locationPattern);
			Resource dirRoot = classRoot.createRelative(directory);
			if (dirRoot.exists()) {
				List<Resource> finder = new ArrayList<Resource>();
				boolean recursive = getPathMatcher().isPattern(locationPattern);
				Collection<?> files = FileUtils.listFiles(dirRoot.getFile(), null,
					recursive);
				for (Object object : files) {
					if (!(object instanceof File)) {
						continue;
					}
					File file = (File) object;
					String absolutePath = StringUtils.cleanPath(file.getAbsolutePath());
					String relativePath = absolutePath.substring(rootPath.length());
					if (getPathMatcher().match(locationPattern, relativePath)) {
						finder.add(new FileSystemResource(file));
					}
				}
				return finder.toArray(new Resource[finder.size()]);
			} else {
				return null;
			}
		}
	}

	public JarResourceLoader getJarResourceLoader() {
		return this.jarResourceLoader;
	}

	public boolean hasResource(Class<?> clazz) {
		String entry = ClassUtils.convertClassNameToResourcePath(clazz.getName())
					+ ".class";
		return hasResource(entry);
	}

	public boolean hasResource(String path) {
		if (!jarResourceLoader.hasJarResource()) {
			return false;
		}
		return getJarResourceLoader().hasEntry(path);
	}

	protected Resource[] findPathMatchingJarResources(String locationPattern) {
		List<Resource> result = new ArrayList<Resource>(16);
		JarResourceLoader jarResourceLoader = (JarResourceLoader) getResourceLoader();
		Set<String> entriesPath = jarResourceLoader.getEntryFilesByDir(
			locationPattern, getPathMatcher());
		if (entriesPath == null) {
			return result.toArray(new Resource[result.size()]);
		}
		for (String entryPath : entriesPath) {
			String path = "/" + entryPath;
			if (getPathMatcher().match(locationPattern, path)) {
				Resource resource = getResource(path);
				if (resource == null) {
					continue;
				}
				result.add(resource);
			}
		}
		return result.toArray(new Resource[result.size()]);
	}
}
