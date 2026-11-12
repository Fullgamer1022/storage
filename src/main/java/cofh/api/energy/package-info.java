/**
 * Declara a API de energia RF como "CoFHAPI|energy". O Ender IO (e outros mods) so ativam
 * as interfaces IEnergyReceiver/IEnergyProvider nos blocos deles se esse modid existir;
 * sem CoFH Core instalado, isso e o que fazia o Flux nao enxergar os blocos do Ender IO.
 */
@API(apiVersion = "1.0.0", owner = "CoFHAPI", provides = "CoFHAPI|energy")
package cofh.api.energy;

import cpw.mods.fml.common.API;
