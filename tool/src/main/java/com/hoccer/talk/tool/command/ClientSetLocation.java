package com.hoccer.talk.tool.command;


import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;


@CLICommand(name = "csetlocation", description = "Set the geo-location of a client, " +
                                                 "use: csetlocation -c <client-id> -long <longitude> -lat <latitude>")
public class ClientSetLocation extends TalkToolClientCommand {

    @Parameter(description = "Longitude", names = "-long", required = true)
    Double pLongitude;

    @Parameter(description = "Latitude", names = "-lat", required = true)
    Double pLatitude;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {
        client.setGeoLocation(pLongitude, pLatitude);
    }
}
