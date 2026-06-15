# Edu Report Pro — Firebase Data Structure

Place your `google-services.json` in **`app/google-services.json`** (already configured in Gradle).

## Collections

### `teachers` (document id = Firebase Auth UID)
| Field | Type | Description |
|-------|------|-------------|
| id | string | Same as UID |
| name | string | Teacher full name |
| email | string | |
| phone | string | |
| schoolIds | array | Optional school references |

### `schools`
| Field | Type | Description |
|-------|------|-------------|
| id | string | Auto ID |
| teacherId | string | Owner teacher |
| name | string | School name |
| udiseCode | string | |
| board | string | CBSE / State / ICSE |

### `academic_years`
| Field | Type | Description |
|-------|------|-------------|
| id | string | Auto ID |
| teacherId | string | |
| schoolId | string | Optional |
| label | string | e.g. `2026-27` |
| startYear | number | 2026 |
| endYear | number | 2027 |
| active | boolean | |

### `semesters`
| Field | Type | Description |
|-------|------|-------------|
| id | string | |
| yearId | string | Parent academic year |
| teacherId | string | |
| number | number | 1 or 2 |
| name | string | First Semester |
| subtitle | string | Easy Reports |

### `classes`
| Field | Type | Description |
|-------|------|-------------|
| id | string | |
| schoolId | string | |
| yearId | string | Links to academic year |
| academicYearLabel | string | Denormalized |
| semesterId | string | |
| className | string | 1–12 |
| division | string | A, B, … |
| examName | string | |
| year | number | Legacy numeric year |
| subjects | array | `{ name, maxMarks }` |
| teacherName | string | Optional display |
| studentCount | number | Optional |

### `students`
| Field | Type | Description |
|-------|------|-------------|
| id | string | |
| teacherId | string | Required for teacher queries |
| schoolId | string | |
| classId | string | |
| name | string | |
| rollNo | string | |
| dob | string | |
| gender | string | |
| parentName | string | |
| schoolName | string | Denormalized |
| className | string | Denormalized |

### `marks`
| Field | Type | Description |
|-------|------|-------------|
| studentId | string | |
| classId | string | |
| subjectMarks | map | |
| editedBy | string | |

## App navigation flow

1. **Drawer** — CCE-style menu (About Student, Class Level, School Level).
2. **Info & Print Setting** — Year → Semester → Class selectors (start screen).
3. **Class & Div** — List classes for selected year; add class via FAB.
4. **Add Student** — `StudentRegisterActivity` uses current session school/class.
5. **Reports** — Existing marksheet / PDF flow.

## Firestore indexes

Composite indexes may be required if you add `orderBy` on filtered queries. Current code sorts in memory to avoid index setup.

## Security rules (starter)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{collection}/{docId} {
      allow read, write: if request.auth != null
        && (resource == null || resource.data.teacherId == request.auth.uid
            || collection == 'teachers' && docId == request.auth.uid);
    }
  }
}
```

Tune rules before production.
