package me.daegyeo.netflixchecker.command

import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import discord4j.rest.service.ApplicationService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CommandRegistrar(
    private val restClient: RestClient,
    private val commandRegistry: List<ICommand>
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(CommandRegistrar::class.java)

    override fun run(args: ApplicationArguments) {
        val applicationService: ApplicationService = restClient.applicationService
        val applicationId: Long? = restClient.applicationId.block()

        val commands: ArrayList<ApplicationCommandRequest> = arrayListOf()
        commandRegistry.forEach {
            commands.add(
                ApplicationCommandRequest.builder()
                    .name(it.name)
                    .description(it.description)
                    .addAllOptions(it.options.map { option ->
                        ApplicationCommandOptionData.builder()
                            .name(option.name)
                            .description(option.description)
                            .type(option.type)
                            .required(option.required)
                            .build()
                    })
                    .build()
            )
        }

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId!!, commands)
            .doOnNext { logger.info("명령어를 등록했습니다.") }
            .doOnError { logger.error("명령어를 등록하는 중 오류가 발생했습니다.", it) }
            .subscribe()
    }
}