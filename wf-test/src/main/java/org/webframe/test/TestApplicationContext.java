/*
 * wf-test
 * Created on 2012-2-25-下午12:35:34
 */

package org.webframe.test;

import org.springframework.context.ApplicationContext;
import org.webframe.support.util.ApplicationContextSupport;

/**
 * TestApplicationContext
 * 
 * @author <a href="mailto:guoqing.huang@foxmail.com">黄国庆 </a>
 * @since 2012-2-25 下午12:35:34
 * @version
 */
public final class TestApplicationContext extends ApplicationContextSupport {

	private static ApplicationContext	ac	= null;

	private TestApplicationContext() {
	}

	static void init(ApplicationContext applicationContext) {
		ac = applicationContext;
		initAc(applicationContext);
	}

	static ApplicationContext getApplicationContext() {
		if (ac == null) {
			ac = getAc();
		}
		return ac;
	}
}
