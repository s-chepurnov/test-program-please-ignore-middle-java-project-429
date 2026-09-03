import "@typespec/http";
import "@typespec/openapi";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

@service(#{ title: "Flight Booking API" })
@info(#{ version: "1.0.0" })
@server("http://localhost:8080", "Локальный запуск: фронтенд и API на одном порту")
namespace FlightBookingApi;

// ---------- Справочные данные ----------

@doc("Город (аэропорт) для формы поиска. code — IATA-подобный код, напр. MOW/LED.")
@example(#{ code: "MOW", name: "Москва", country: "Россия" })
model City {
  code: string;
  name: string;
  country?: string;
}

@doc("Денежная сумма. amount — целые рубли (без копеек).")
@example(#{ amount: 5400, currency: "RUB" })
model Money {
  amount: int32;
  currency: string = "RUB";
}

@doc("Авиакомпания.")
@example(#{ code: "SU", name: "Аэрофлот" })
model Airline {
  code: string;
  name: string;
}

@doc("Рейс — результат поиска и предмет бронирования.")
@example(#{
  id: "fl_1",
  flightNumber: "SU1234",
  airline: #{ code: "SU", name: "Аэрофлот" },
  origin: #{ code: "MOW", name: "Москва", country: "Россия" },
  destination: #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
  departureAt: utcDateTime.fromISO("2026-07-01T08:00:00Z"),
  arrivalAt: utcDateTime.fromISO("2026-07-01T09:25:00Z"),
  durationMinutes: 85,
  price: #{ amount: 5400, currency: "RUB" },
  seatsAvailable: 42,
})
model Flight {
  id: string;
  flightNumber: string;
  airline: Airline;
  origin: City;
  destination: City;
  departureAt: utcDateTime;
  arrivalAt: utcDateTime;
  durationMinutes: int32;

  @doc("Цена за одного пассажира.")
  price: Money;

  seatsAvailable: int32;
}

// ---------- Бронирование ----------

@doc("Контактные данные заказчика брони.")
@example(#{ email: "ivan@example.com", phone: "+79991234567" })
model Contact {
  email: string;
  phone: string;
}

@doc("Пассажир.")
@example(#{
  firstName: "Иван",
  lastName: "Петров",
  dateOfBirth: plainDate.fromISO("1990-05-20"),
  documentNumber: "4509 123456",
})
model Passenger {
  firstName: string;
  lastName: string;
  dateOfBirth: plainDate;
  documentNumber: string;
}

@doc("Статус брони.")
enum BookingStatus {
  confirmed,
  cancelled,
}

@doc("Тело запроса на создание брони.")
model CreateBookingRequest {
  flightId: string;
  contact: Contact;

  @minItems(1)
  @doc("Список пассажиров — минимум один.")
  passengers: Passenger[];
}

@doc("Тело запроса на отмену брони: второй фактор доступа.")
@example(#{ lastName: "Петров" })
model LastNameBody {
  @doc("Фамилия любого пассажира брони. Отсутствующая или неверная даёт 404, а не 400.")
  lastName?: string;
}

@doc("Бронь. Возвращается при создании и просмотре.")
@example(#{
  code: "AB1234",
  status: BookingStatus.confirmed,
  flight: #{
    id: "fl_1",
    flightNumber: "SU1234",
    airline: #{ code: "SU", name: "Аэрофлот" },
    origin: #{ code: "MOW", name: "Москва", country: "Россия" },
    destination: #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
    departureAt: utcDateTime.fromISO("2026-07-01T08:00:00Z"),
    arrivalAt: utcDateTime.fromISO("2026-07-01T09:25:00Z"),
    durationMinutes: 85,
    price: #{ amount: 5400, currency: "RUB" },
    seatsAvailable: 42,
  },
  passengers: #[
    #{
      firstName: "Иван",
      lastName: "Петров",
      dateOfBirth: plainDate.fromISO("1990-05-20"),
      documentNumber: "4509 123456",
    }
  ],
  contact: #{ email: "ivan@example.com", phone: "+79991234567" },
  totalPrice: #{ amount: 5400, currency: "RUB" },
  createdAt: utcDateTime.fromISO("2026-06-25T12:00:00Z"),
})
model Booking {
  @doc("Уникальный код брони (PNR-подобный, ~6 символов).")
  code: string;

  status: BookingStatus;
  flight: Flight;
  passengers: Passenger[];
  contact: Contact;

  @doc("Итоговая стоимость: цена рейса × число пассажиров.")
  totalPrice: Money;

  createdAt: utcDateTime;
}

// ---------- Ошибки ----------

@error
@doc("Ресурс не найден (404). Также — при отсутствующем или неверном lastName к коду брони.")
@example(#{ statusCode: 404, code: "not_found", message: "Бронь с таким кодом не найдена" })
model NotFoundError {
  @statusCode statusCode: 404;
  code: string = "not_found";
  message: string;
}

@error
@doc("Некорректные параметры запроса или тело (400).")
@example(#{ statusCode: 400, code: "validation_error", message: "Укажите параметр date" })
model BadRequestError {
  @statusCode statusCode: 400;
  code: string = "validation_error";
  message: string;
}

// ---------- Операции ----------

@route("/api/cities")
@tag("Reference")
interface Cities {
  @doc("Справочник городов для автодополнения в форме поиска.")
  @opExample(
    #{
      returnType: #[
        #{ code: "MOW", name: "Москва", country: "Россия" },
        #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
        #{ code: "AER", name: "Сочи", country: "Россия" },
        #{ code: "KZN", name: "Казань", country: "Россия" },
        #{ code: "SVX", name: "Екатеринбург", country: "Россия" }
      ],
    },
    #{ title: "Список городов" }
  )
  @get
  list(): City[];
}

@route("/api/flights")
@tag("Flights")
interface Flights {
  @doc("Поиск рейсов. Пустой результат — 200 с []. Некорректные параметры — 400.")
  @opExample(
    #{
      parameters: #{
        origin: "MOW",
        destination: "LED",
        date: plainDate.fromISO("2026-07-01"),
        passengers: 2,
      },
      returnType: #[
        #{
          id: "fl_1",
          flightNumber: "SU1234",
          airline: #{ code: "SU", name: "Аэрофлот" },
          origin: #{ code: "MOW", name: "Москва", country: "Россия" },
          destination: #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
          departureAt: utcDateTime.fromISO("2026-07-01T08:00:00Z"),
          arrivalAt: utcDateTime.fromISO("2026-07-01T09:25:00Z"),
          durationMinutes: 85,
          price: #{ amount: 5400, currency: "RUB" },
          seatsAvailable: 42,
        },
        #{
          id: "fl_2",
          flightNumber: "DP202",
          airline: #{ code: "DP", name: "Победа" },
          origin: #{ code: "MOW", name: "Москва", country: "Россия" },
          destination: #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
          departureAt: utcDateTime.fromISO("2026-07-01T13:30:00Z"),
          arrivalAt: utcDateTime.fromISO("2026-07-01T15:00:00Z"),
          durationMinutes: 90,
          price: #{ amount: 3200, currency: "RUB" },
          seatsAvailable: 18,
        },
        #{
          id: "fl_3",
          flightNumber: "S7311",
          airline: #{ code: "S7", name: "S7 Airlines" },
          origin: #{ code: "MOW", name: "Москва", country: "Россия" },
          destination: #{ code: "LED", name: "Санкт-Петербург", country: "Россия" },
          departureAt: utcDateTime.fromISO("2026-07-01T19:45:00Z"),
          arrivalAt: utcDateTime.fromISO("2026-07-01T21:15:00Z"),
          durationMinutes: 90,
          price: #{ amount: 4700, currency: "RUB" },
          seatsAvailable: 7,
        }
      ],
    },
    #{ title: "Поиск Москва → Санкт-Петербург" }
  )
  @get
  search(
    @query origin: string,
    @query destination: string,
    @query date: plainDate,
    @query passengers?: int32 = 1,
  ): Flight[] | BadRequestError;

  @doc("Получить рейс по id (рефреш/диплинк на странице оформления).")
  @get
  @route("/{id}")
  getById(@path id: string): Flight | NotFoundError;
}

@route("/api/bookings")
@tag("Bookings")
interface Bookings {
  @doc("Создать бронь. Пустой passengers[] или нет обязательных полей — 400. Ответ 201 с code.")
  @post
  create(@body body: CreateBookingRequest): {
    @statusCode statusCode: 201;
    @body booking: Booking;
  } | BadRequestError;

  @doc("""
    Просмотр брони по коду + фамилии.
    """)
  @get
  @route("/{code}")
  getByCode(
    @path code: string,
    @query lastName?: string,
  ): Booking | NotFoundError;

  @doc("""
    Отмена брони по code + lastName. Возвращает бронь со status=cancelled. Неверные данные → 404.
    """)
  @post
  @route("/{code}/cancel")
  cancel(
    @path code: string,
    @body body?: LastNameBody,
  ): Booking | NotFoundError;
}
