/*
 * wf-core
 * Created on 2012-2-2-上午09:37:22
 */

package org.webframe.core.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * Identity主键策略实体类接口
 * 
 * @author <a href="mailto:guoqing.huang@foxmail.com">黄国庆 </a>
 * @since 2012-2-2 上午09:37:22
 * @version
 */
public interface IIdentityEntity {

	@Id
	@GeneratedValue(generator = "identity")
	@GenericGenerator(name = "identity", strategy = "identity")
	@Column(name = "ID_", length = 32, updatable = false, insertable = false)
	public String getId();
}
