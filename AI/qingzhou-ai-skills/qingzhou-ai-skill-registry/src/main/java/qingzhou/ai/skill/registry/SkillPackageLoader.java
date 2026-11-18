package qingzhou.ai.skill.registry;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.common.Configs;
import qingzhou.logger.Logger;

/**
 * 按 conf/skills 下的技能包动态注册技能：投放 zip 即可上线
 */
@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class SkillPackageLoader {
    @Reference
    private Logger logger;

    private final List<ServiceRegistration<?>> registrations = new ArrayList<>();

    @Activate
    public void init(ComponentContext context, Map<String, String> config) throws IOException {
        Configs configs = new Configs(config);
        int maxBytes = (int) configs.between("skill_doc_max_bytes", 1024, 1024 * 1024, 96 * 1024);
        Path skillsDir = Paths.get(configs.notBlank("qingzhou.instance"), "conf", "skills");
        if (!Files.isDirectory(skillsDir)) {
            logger.info("no skill packages: " + skillsDir);
            return;
        }

        // 先全部解析再统一注册：解析期异常不会留下半套已注册技能
        List<SkillPackage> packages = new ArrayList<>();
        Set<String> names = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skillsDir, "*.zip")) {
            for (Path file : stream) {
                try {
                    SkillPackage skill = SkillPackage.load(file, maxBytes);
                    if (!names.add(skill.name())) {
                        logger.warn("skip duplicated skill name [" + skill.name() + "] in " + file.getFileName());
                        continue;
                    }
                    packages.add(skill);
                } catch (Exception e) {
                    logger.warn("skip invalid skill package [" + file.getFileName() + "]: " + e.getMessage());
                }
            }
        }

        BundleContext bundleContext = context.getBundleContext();
        for (SkillPackage skill : packages) {
            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(SkillService.SKILL_NAME, skill.name());
            properties.put(SkillService.SKILL_DESCRIPTION, skill.description());
            registrations.add(bundleContext.registerService(SkillService.class, skill, properties));
            logger.info("loaded skill package [" + skill.name() + "]");
        }
    }

    @Deactivate
    public void destroy() {
        registrations.forEach(ServiceRegistration::unregister);
    }
}
