//
//  Extensions.swift
//  iosApp
//
//  Created by Zeeshan Ali on 25/01/2026.
//

extension String {
    func deletingSuffix(_ suffix: String) -> String {
        guard self.hasSuffix(suffix) else { return self }
        return String(self.dropLast(suffix.count))
    }
}
