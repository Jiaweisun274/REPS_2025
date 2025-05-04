package reps.models

import java.time.ZonedDateTime

case class Source(id: String)

case class Record(
                   timestamp: ZonedDateTime,
                   source: Source,
                   outputKWh: Double
                 )