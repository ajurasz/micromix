package micromix.boot.spring

import java.util.{List => JList, Map => JMap, Collections}
import Collections._

import scala.collection.JavaConversions._
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.boot.builder.ParentContextApplicationContextInitializer

trait SpringBootSupport {

  // Members

  protected var context: ApplicationContext = _

  protected val cachedProperties = properties

  // Configuration callbacks

  protected def properties: JMap[String, Any] =
    emptyMap()

  protected def parentContext: ApplicationContext = null

  protected def isWebApp: Boolean =
    false

  // Beans configuration callbacks

  def configurationClasses: JList[Class[_]] =
    emptyList()

  def namedBeansDefinitions: JMap[String, Class[_]] =
    emptyMap()

  def singletons: JList[_] =
    emptyList()

  // Initialization

  def initialize() {
    val microMixApplicationBuilder = new MicroMixApplicationBuilder().web(isWebApp)
    val applicationBuilder = microMixApplicationBuilder.toSpringApplicationBuilder
    applicationBuilder.sources(configurationClasses: _*)
    Option(parentContext).foreach(parent => applicationBuilder.initializers(new ParentContextApplicationContextInitializer(parent)))
    cachedProperties.foreach(property => System.setProperty(property._1, property._2.toString))
    applicationBuilder.initializers(new AnnotatedBeanDefinitionApplicationContextInitializer(namedBeansDefinitions))
    context = applicationBuilder.run()
    context.getBeansOfType(classOf[BootCallback]).values().foreach(_.afterBoot())
    singletons.foreach {
      bean =>
        val beanRegistry = context.getAutowireCapableBeanFactory.asInstanceOf[ConfigurableListableBeanFactory]
        beanRegistry.autowireBean(bean)
        beanRegistry.registerSingleton(generateBeanName(bean), bean)
    }
  }

  def autowire() {
    context.getAutowireCapableBeanFactory.autowireBean(this)
  }

  protected def generateBeanName(bean: Any) =
    bean.getClass.getName

}