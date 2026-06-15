export function getDefaultSubjectsForClass(className) {
  const std = parseInt(className, 10);
  const subjects = [];

  if (isNaN(std) || std < 1 || std > 10) {
    return ["Marathi", "English", "Mathematics", "Science"];
  }

  // 1. First Language (Marathi)
  subjects.push("Marathi");

  // 2. Second Language (English)
  subjects.push("English");

  if (std >= 5) {
    // 3. Third Language (Hindi)
    subjects.push("Hindi");
  }

  // 4. Mathematics
  subjects.push("Mathematics");

  if (std === 1 || std === 2) {
    subjects.push("Play, Do, Learn");
  } else if (std === 3 || std === 4) {
    subjects.push("Environmental Studies");
    subjects.push("Play, Do, Learn");
  } else if (std === 5) {
    subjects.push("Environmental Studies Part 1");
    subjects.push("Environmental Studies Part 2");
    subjects.push("Health & Physical Education");
    subjects.push("Work Experience");
    subjects.push("Art");
  } else if (std >= 6 && std <= 8) {
    subjects.push("Science");
    subjects.push("History and Civics");
    subjects.push("Geography");
    subjects.push("Health & Physical Education");
    subjects.push("Work Experience");
    subjects.push("Art");
  } else {
    // 9-10
    subjects.push("Science");
    subjects.push("History and Civics");
    subjects.push("Geography");
    subjects.push("Health & Physical Education");
    subjects.push("Work Experience");
    subjects.push("Art");
    subjects.push("Information & Comm. Technology (ICT)");
    subjects.push("Water Security & Environment Studies");
  }

  return subjects;
}
