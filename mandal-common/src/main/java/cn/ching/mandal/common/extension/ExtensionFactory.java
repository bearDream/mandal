package cn.ching.mandal.common.extension;

/**
 * 2018/1/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface ExtensionFactory {

    /**
     * get extension class
     * @param type  extension type
     * @param name  extension name
     * @return  instance
     */
    <T> T getExtension(Class<T> type, String name);
}
