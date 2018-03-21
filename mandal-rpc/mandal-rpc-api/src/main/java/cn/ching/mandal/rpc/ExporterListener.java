package cn.ching.mandal.rpc;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface ExporterListener {

    /**
     * the exporter exported
     * @param exporter
     */
    void exporter(Exporter<?> exporter);

    /**
     * the exporter unexporter.
     * @param exporter
     */
    void unexporter(Exporter<?> exporter);
}
