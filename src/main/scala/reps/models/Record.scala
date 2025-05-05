package reps.models

// Models for energy data records and their sources

import java.time.ZonedDateTime

case class Source(id: String)
case class Record(
                   timestamp: ZonedDateTime,
                   source: Source,
                   outputKWh: Double
                 )